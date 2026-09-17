package com.autoadskipper.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.autoadskipper.MainActivity
import com.autoadskipper.R
import com.autoadskipper.data.AppPreferencesRepository
import com.autoadskipper.data.DataStoreManager
import com.autoadskipper.data.StatisticsRepository
import com.autoadskipper.data.model.AppSettings
import com.autoadskipper.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The heart of Auto Ad Skipper.
 *
 * - Scans every currently visible window (via [getWindows]), not just
 *   `rootInActiveWindow`, so detection keeps working when the notification
 *   shade/quick settings are open and inside Picture-in-Picture/floating
 *   windows. System windows (status bar, launcher, input method, our own
 *   package) are filtered out before any node retrieval to avoid wasted work.
 * - Only acts on explicit text / resource-id / close-dismiss-phrase matches
 *   (see [AdDetector]) — there is no "click any three-dot menu" behavior;
 *   that was tried, found to click unrelated UI in other apps, and removed.
 * - Throttled: a burst of accessibility events (very common during video
 *   playback) triggers at most one full scan per [Constants.MIN_SCAN_INTERVAL_MS],
 *   which meaningfully cuts CPU/battery cost without adding perceptible
 *   click latency.
 */
@AndroidEntryPoint
class AdSkipAccessibilityService : AccessibilityService() {

    @Inject lateinit var dataStoreManager: DataStoreManager
    @Inject lateinit var statisticsRepository: StatisticsRepository
    @Inject lateinit var appPreferencesRepository: AppPreferencesRepository
    @Inject lateinit var detectionCache: DetectionCache
    @Inject lateinit var positionMatchTracker: PositionMatchTracker
    @Inject lateinit var visualDetector: SkipButtonVisualDetector

    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        // Swallow and continue — a single failed event must never take the
        // whole monitoring loop down.
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + exceptionHandler)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastClickSignature: String? = null
    private var lastClickTimeMs: Long = 0L
    private var lastScanTimeMs: Long = 0L
    private var pendingClickJob: Job? = null
    private var soundPool: SoundPool? = null

    // Cached copies of settings/disabled-packages, kept current by a single
    // background collector (see observeConfigState) rather than each
    // accessibility event separately subscribing to DataStore via .first().
    // DataStore reads have real overhead (disk-backed, coroutine dispatch);
    // doing that on every single event — which can fire many times a second
    // during video playback — was an avoidable cost on the hot path.
    @Volatile private var cachedSettings: AppSettings = AppSettings()
    @Volatile private var cachedDisabledPackages: Set<String> = emptySet()

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "android",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        packageNameSelf
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        setupSoundPool()
        observeConfigState()
        observeMonitoringNotificationState()
    }

    private fun observeConfigState() {
        serviceScope.launch {
            dataStoreManager.settingsFlow.collect { cachedSettings = it }
        }
        serviceScope.launch {
            appPreferencesRepository.disabledPackagesFlow.collect { cachedDisabledPackages = it }
        }
    }

    private fun setupSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build()
    }

    private fun observeMonitoringNotificationState() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                dataStoreManager.settingsFlow,
                statisticsRepository.statisticsFlow
            ) { settings, stats -> settings.monitoringPaused to stats.todaySkipped }
                .collect { (paused, todayCount) ->
                    updateMonitoringNotification(todayCount = todayCount, paused = paused)
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                // Window-state changes (app switches) always get through
                // immediately; content-change bursts (very frequent during
                // video playback) are throttled.
                val now = System.currentTimeMillis()
                val isDiscreteEvent = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                if (isDiscreteEvent || now - lastScanTimeMs >= Constants.MIN_SCAN_INTERVAL_MS) {
                    lastScanTimeMs = now
                    handlePossibleAd()
                }
            }
            else -> Unit
        }
    }

    private data class WindowCandidate(val packageName: String, val root: AccessibilityNodeInfo)

    private fun collectWindowCandidates(): List<WindowCandidate> {
        val candidates = mutableListOf<WindowCandidate>()
        val windowList = runCatching { windows }.getOrNull()

        if (!windowList.isNullOrEmpty()) {
            for (window in windowList) {
                val root = runCatching { window.root }.getOrNull() ?: continue
                val pkg = root.packageName?.toString()
                if (pkg == null || pkg in ignoredPackages) {
                    runCatching { root.recycle() }
                    continue
                }
                candidates.add(WindowCandidate(pkg, root))
            }
        }

        if (candidates.isEmpty()) {
            val root = runCatching { rootInActiveWindow }.getOrNull()
            val pkg = root?.packageName?.toString()
            if (root != null && pkg != null && pkg !in ignoredPackages) {
                candidates.add(WindowCandidate(pkg, root))
            }
        }
        return candidates
    }

    private fun getScreenBounds(): Rect {
        val metrics = resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun handlePossibleAd() {
        val startTime = System.currentTimeMillis()

        // Dispatched onto the scope's default background dispatcher so the
        // (potentially multi-window) tree walk never runs on the main
        // thread — accessibility events are delivered on the main looper,
        // and this work should never risk janking the UI. The settings and
        // disabled-package values themselves are read from the in-memory
        // cache (no flow re-subscription), which is the actual fix versus
        // the previous per-event .first() calls.
        serviceScope.launch {
            val settings = cachedSettings
            if (!settings.serviceEnabled || settings.monitoringPaused) return@launch

            val disabledPackages = cachedDisabledPackages
            val screenBounds = getScreenBounds()
            val candidates = runCatching { collectWindowCandidates() }.getOrDefault(emptyList())

            for (candidate in candidates) {
                if (candidate.packageName in disabledPackages) {
                    runCatching { candidate.root.recycle() }
                    continue
                }

                // Generic (confidence-gated) matches are suppressed for a
                // moment after an app opens: onboarding/tutorial "Skip"
                // buttons cluster right after launch, while real in-app ads
                // appear later during use.
                val settledLongEnough = (System.currentTimeMillis() -
                    (foregroundSince[candidate.packageName] ?: 0L)) >= Constants.GENERIC_MATCH_FOREGROUND_GRACE_MS

                val match = runCatching {
                    AdDetector.findSkipNode(
                        root = candidate.root,
                        packageName = candidate.packageName,
                        sensitivity = settings.sensitivity,
                        cache = detectionCache,
                        positionTracker = positionMatchTracker,
                        screenBounds = screenBounds,
                        allowGenericMatches = settledLongEnough
                    )
                }.getOrNull()

                if (match != null) {
                    val signature = "${candidate.packageName}|${match.identifier}"
                    if (!isDebounced(signature) && !isRateLimited()) {
                        val latency = System.currentTimeMillis() - startTime
                        scheduleClick(candidate.packageName, match.node, settings, latency, signature)
                        runCatching { candidate.root.recycle() }
                        return@launch
                    }
                }
                runCatching { candidate.root.recycle() }
            }
        }
    }

    /**
     * Hard ceiling on click frequency, independent of what detection
     * believes it found. If the engine somehow latches onto a recurring
     * non-ad element, this stops it from clicking repeatedly.
     */
    @Synchronized
    private fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        while (recentClickTimes.isNotEmpty() && now - recentClickTimes.first() > Constants.CLICK_RATE_WINDOW_MS) {
            recentClickTimes.removeFirst()
        }
        return recentClickTimes.size >= Constants.MAX_CLICKS_PER_WINDOW
    }

    @Synchronized
    private fun recordClickTime() {
        recentClickTimes.addLast(System.currentTimeMillis())
    }

    private fun isDebounced(signature: String): Boolean {
        val now = System.currentTimeMillis()
        return signature == lastClickSignature && (now - lastClickTimeMs) < Constants.CLICK_DEBOUNCE_MS
    }

    private fun scheduleClick(
        packageName: String,
        node: AccessibilityNodeInfo,
        settings: AppSettings,
        detectionLatencyMs: Long,
        debounceSignature: String
    ) {
        pendingClickJob?.cancel()
        mainHandler.postDelayed({
            performClick(packageName, node, settings, detectionLatencyMs, debounceSignature)
        }, settings.clickDelayMs.coerceIn(Constants.MIN_CLICK_DELAY_MS, Constants.MAX_CLICK_DELAY_MS))
    }

    private fun performClick(
        packageName: String,
        node: AccessibilityNodeInfo,
        settings: AppSettings,
        detectionLatencyMs: Long,
        debounceSignature: String
    ) {
        val success = runCatching { node.performAction(AccessibilityNodeInfo.ACTION_CLICK) }.getOrDefault(false)
        runCatching { node.recycle() }

        serviceScope.launch { statisticsRepository.recordAttempt(success, detectionLatencyMs) }

        if (!success) return

        lastClickSignature = debounceSignature
        lastClickTimeMs = System.currentTimeMillis()
        recordClickTime()

        val appLabel = resolveAppLabel(packageName)
        serviceScope.launch { statisticsRepository.recordSkip(appLabel, packageName) }

        if (settings.vibrationEnabled) vibrateOnSkip()
        if (settings.soundEnabled) playSkipSound()
        if (settings.notificationEnabled) {
            showEventNotification(getString(R.string.notif_ad_skipped_in, appLabel))
        }
    }

    private fun resolveAppLabel(packageName: String): String {
        return runCatching {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private fun vibrateOnSkip() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(60)
                }
            }
        }
    }

    private fun playSkipSound() {
        runCatching {
            android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80)
                .startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
        }
    }

    private fun showEventNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching { manager.notify(Constants.NOTIFICATION_ID, notification) }
    }

    /**
     * Persistent, ongoing "Monitoring" notification with Pause/Resume
     * actions. Not a foreground-service notification — this is an
     * accessibility service, already kept alive by the OS — it's a
     * low-priority ongoing notification purely for transparency and quick
     * control.
     */
    fun updateMonitoringNotification(todayCount: Int, paused: Boolean) {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val actionIntent = Intent(
            if (paused) Constants.ACTION_RESUME_MONITORING else Constants.ACTION_PAUSE_MONITORING
        ).setPackage(packageName)
        val actionPendingIntent = PendingIntent.getBroadcast(
            this, 0, actionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = if (paused) {
            getString(R.string.notif_monitoring_paused)
        } else {
            getString(R.string.notif_monitoring_active, todayCount)
        }
        val actionLabel = if (paused) getString(R.string.notif_action_resume) else getString(R.string.notif_action_pause)

        val notification = NotificationCompat.Builder(this, Constants.MONITORING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
            .addAction(0, actionLabel, actionPendingIntent)
            .build()

        runCatching { manager.notify(Constants.MONITORING_NOTIFICATION_ID, notification) }
    }

    override fun onInterrupt() {
        // No-op: nothing to clean up mid-gesture.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingClickJob?.cancel()
        soundPool?.release()
        detectionCache.clear()
    }

    companion object {
        private const val packageNameSelf = "com.autoadskipper"
    }
}
