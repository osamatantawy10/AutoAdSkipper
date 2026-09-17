package com.autoadskipper.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoadskipper.data.model.DailyCount
import com.autoadskipper.data.model.SkipStatistics
import com.autoadskipper.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.statsDataStore by preferencesDataStore(name = "auto_ad_skipper_stats")

/**
 * Owns all skip-counter state: today's count (with automatic day-rollover),
 * lifetime total, last-skipped app, per-app frequency, a rolling daily
 * history for the weekly/monthly charts on the Stats screen, and detection
 * quality metrics (attempts vs. successful clicks, rolling average detection
 * latency) used for the "detection success rate" / "detection speed" cards.
 */
@Singleton
class StatisticsRepository @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private object Keys {
        val TOTAL = intPreferencesKey(Constants.KEY_TOTAL_SKIPPED)
        val TODAY = intPreferencesKey(Constants.KEY_TODAY_SKIPPED)
        val TODAY_DATE = stringPreferencesKey(Constants.KEY_TODAY_DATE)
        val LAST_APP = stringPreferencesKey(Constants.KEY_LAST_SKIPPED_APP)
        val LAST_PACKAGE = stringPreferencesKey(Constants.KEY_LAST_SKIPPED_PACKAGE)
        val LAST_TIME = longPreferencesKey(Constants.KEY_LAST_SKIPPED_TIME)
        val HISTORY_JSON = stringPreferencesKey(Constants.KEY_DAILY_HISTORY_JSON)
        val FREQUENCY_JSON = stringPreferencesKey(Constants.KEY_APP_FREQUENCY_JSON)
        val ATTEMPT_COUNT = intPreferencesKey(Constants.KEY_ATTEMPT_COUNT)
        val SUCCESS_COUNT = intPreferencesKey(Constants.KEY_SUCCESS_COUNT)
        val AVG_DETECTION_MS = longPreferencesKey(Constants.KEY_AVG_DETECTION_MS)
    }

    private fun todayKey(): String = dateFormat.format(Date())

    val statisticsFlow: Flow<SkipStatistics> = context.statsDataStore.data.map { prefs ->
        val storedDate = prefs[Keys.TODAY_DATE]
        val todayCount = if (storedDate == todayKey()) prefs[Keys.TODAY] ?: 0 else 0

        val history = prefs[Keys.HISTORY_JSON]?.let {
            runCatching { json.decodeFromString<List<DailyCount>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()

        val frequency = prefs[Keys.FREQUENCY_JSON]?.let {
            runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrDefault(emptyMap())
        } ?: emptyMap()

        SkipStatistics(
            todaySkipped = todayCount,
            totalSkipped = prefs[Keys.TOTAL] ?: 0,
            lastSkippedApp = prefs[Keys.LAST_APP],
            lastSkippedPackage = prefs[Keys.LAST_PACKAGE],
            lastSkippedTime = prefs[Keys.LAST_TIME],
            dailyHistory = history,
            appFrequency = frequency,
            attemptCount = prefs[Keys.ATTEMPT_COUNT] ?: 0,
            successCount = prefs[Keys.SUCCESS_COUNT] ?: 0,
            avgDetectionMs = prefs[Keys.AVG_DETECTION_MS] ?: 0L
        )
    }

    /**
     * Called by the accessibility service every time it *attempts* a click
     * (whether or not the click reports success), so the success-rate metric
     * reflects real-world reliability. [detectionLatencyMs] is the time from
     * "event received" to "node found", folded into a rolling average.
     */
    suspend fun recordAttempt(succeeded: Boolean, detectionLatencyMs: Long) {
        context.statsDataStore.edit { prefs ->
            val attempts = (prefs[Keys.ATTEMPT_COUNT] ?: 0) + 1
            val successes = (prefs[Keys.SUCCESS_COUNT] ?: 0) + if (succeeded) 1 else 0
            prefs[Keys.ATTEMPT_COUNT] = attempts
            prefs[Keys.SUCCESS_COUNT] = successes

            val prevAvg = prefs[Keys.AVG_DETECTION_MS] ?: 0L
            // Simple rolling average, weighted toward recent samples so a
            // slow first-run doesn't permanently skew the number.
            val newAvg = if (prevAvg == 0L) detectionLatencyMs else ((prevAvg * 4) + detectionLatencyMs) / 5
            prefs[Keys.AVG_DETECTION_MS] = newAvg
        }
    }

    /** Called once a click on a skip button actually succeeds. */
    suspend fun recordSkip(appLabel: String, packageName: String) {
        context.statsDataStore.edit { prefs ->
            val today = todayKey()
            val storedDate = prefs[Keys.TODAY_DATE]
            val currentToday = if (storedDate == today) prefs[Keys.TODAY] ?: 0 else 0

            prefs[Keys.TODAY_DATE] = today
            prefs[Keys.TODAY] = currentToday + 1
            prefs[Keys.TOTAL] = (prefs[Keys.TOTAL] ?: 0) + 1
            prefs[Keys.LAST_APP] = appLabel
            prefs[Keys.LAST_PACKAGE] = packageName
            prefs[Keys.LAST_TIME] = System.currentTimeMillis()

            updateHistory(prefs, today, currentToday + 1)
            updateFrequency(prefs, appLabel)
        }
    }

    private fun updateHistory(prefs: MutablePreferences, today: String, newTodayCount: Int) {
        val existing = prefs[Keys.HISTORY_JSON]?.let {
            runCatching { json.decodeFromString<List<DailyCount>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()

        val withoutToday = existing.filterNot { it.date == today }
        val updated = (withoutToday + DailyCount(today, newTodayCount))
            .sortedByDescending { it.date }
            .take(60) // keep enough history for a real monthly rollup

        prefs[Keys.HISTORY_JSON] = json.encodeToString(updated)
    }

    private fun updateFrequency(prefs: MutablePreferences, appLabel: String) {
        val existing = prefs[Keys.FREQUENCY_JSON]?.let {
            runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrDefault(emptyMap())
        } ?: emptyMap()

        val updated = existing.toMutableMap()
        updated[appLabel] = (updated[appLabel] ?: 0) + 1

        prefs[Keys.FREQUENCY_JSON] = json.encodeToString(updated.toMap())
    }

    suspend fun resetAllStatistics() {
        context.statsDataStore.edit { it.clear() }
    }
}
