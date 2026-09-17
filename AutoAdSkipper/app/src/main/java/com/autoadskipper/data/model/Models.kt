package com.autoadskipper.data.model

import android.graphics.drawable.Drawable
import com.autoadskipper.utils.Constants
import kotlinx.serialization.Serializable

/** Snapshot of user-configurable settings, mirrors DataStore preference values. */
data class AppSettings(
    val serviceEnabled: Boolean = false,
    val monitoringPaused: Boolean = false,
    val clickDelayMs: Long = 300L,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val notificationEnabled: Boolean = true,
    val language: String = "system",
    val themeMode: String = "system",
    val sensitivity: String = "standard",
    val startOnBoot: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val experimentalAiDetectionEnabled: Boolean = false
)

/** Aggregated statistics shown on the Home / Stats screens. */
data class SkipStatistics(
    val todaySkipped: Int = 0,
    val totalSkipped: Int = 0,
    val lastSkippedApp: String? = null,
    val lastSkippedPackage: String? = null,
    val lastSkippedTime: Long? = null,
    val dailyHistory: List<DailyCount> = emptyList(),
    val appFrequency: Map<String, Int> = emptyMap(),
    val attemptCount: Int = 0,
    val successCount: Int = 0,
    val avgDetectionMs: Long = 0L
) {
    val successRatePercent: Int
        get() = if (attemptCount == 0) 100 else ((successCount.toFloat() / attemptCount) * 100).toInt()

    val weeklySkipped: Int
        get() = dailyHistory.sortedByDescending { it.date }.take(7).sumOf { it.count }

    val monthlySkipped: Int
        get() = dailyHistory.sortedByDescending { it.date }.take(30).sumOf { it.count }

    // --- Estimated time saved -------------------------------------------
    // Deliberately derived from the skip counts rather than stored, so it
    // can never drift out of sync with them. This is an ESTIMATE (the app
    // can't know how long any given ad would actually have run) and is
    // labelled as such wherever it's shown.
    val secondsSavedToday: Int
        get() = todaySkipped * Constants.ESTIMATED_SECONDS_SAVED_PER_SKIP
    val secondsSavedTotal: Int
        get() = totalSkipped * Constants.ESTIMATED_SECONDS_SAVED_PER_SKIP
    val secondsSavedWeekly: Int
        get() = weeklySkipped * Constants.ESTIMATED_SECONDS_SAVED_PER_SKIP
}

@Serializable
data class DailyCount(
    val date: String, // yyyy-MM-dd
    val count: Int
)

/** Which detection layer produced a match — surfaced in stats for transparency. */
enum class DetectionLayer {
    TEXT, TEXT_GENERIC_CONFIRMED, RESOURCE_ID, CLOSE_EXPLICIT,
    CLOSE_GENERIC_CONFIRMED, POSITION, VISUAL_AI
}

/** One row on the Supported Apps screen, backed by real installed-app data. */
data class MonitoredAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isMonitoringEnabled: Boolean = true,
    val lastDetectedActivity: String? = null,
    val lastDetectionTime: Long? = null
)

enum class ServiceStatus {
    ENABLED_AND_RUNNING,
    ENABLED_BUT_PAUSED,
    ENABLED_BUT_PERMISSION_MISSING,
    DISABLED
}

/** One row on the System Health screen. */
data class HealthCheckItem(
    val title: String,
    val description: String,
    val isHealthy: Boolean,
    val actionLabel: String? = null
)

data class SystemHealthState(
    val accessibilityGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val notificationsEnabled: Boolean = false
) {
    val allHealthy: Boolean
        get() = accessibilityGranted && batteryOptimizationIgnored && notificationsEnabled
}
