package com.autoadskipper.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoadskipper.data.model.AppSettings
import com.autoadskipper.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auto_ad_skipper_settings")

/**
 * Wraps Jetpack DataStore (Preferences) for all user-configurable settings.
 * Kept separate from [StatisticsRepository], which owns counters/history, so
 * the two concerns don't churn the same file.
 */
@Singleton
class DataStoreManager @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val SERVICE_ENABLED = booleanPreferencesKey(Constants.KEY_SERVICE_ENABLED)
        val MONITORING_PAUSED = booleanPreferencesKey(Constants.KEY_MONITORING_PAUSED)
        val CLICK_DELAY = longPreferencesKey(Constants.KEY_CLICK_DELAY)
        val VIBRATION_ENABLED = booleanPreferencesKey(Constants.KEY_VIBRATION_ENABLED)
        val SOUND_ENABLED = booleanPreferencesKey(Constants.KEY_SOUND_ENABLED)
        val NOTIFICATION_ENABLED = booleanPreferencesKey(Constants.KEY_NOTIFICATION_ENABLED)
        val LANGUAGE = stringPreferencesKey(Constants.KEY_LANGUAGE)
        val THEME_MODE = stringPreferencesKey(Constants.KEY_THEME_MODE)
        val SENSITIVITY = stringPreferencesKey(Constants.KEY_SENSITIVITY)
        val START_ON_BOOT = booleanPreferencesKey(Constants.KEY_START_ON_BOOT)
        val ONBOARDING_COMPLETED = booleanPreferencesKey(Constants.KEY_ONBOARDING_COMPLETED)
        val EXPERIMENTAL_AI = booleanPreferencesKey(Constants.KEY_EXPERIMENTAL_AI_DETECTION)
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serviceEnabled = prefs[Keys.SERVICE_ENABLED] ?: false,
            monitoringPaused = prefs[Keys.MONITORING_PAUSED] ?: false,
            clickDelayMs = prefs[Keys.CLICK_DELAY] ?: Constants.DEFAULT_CLICK_DELAY_MS,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            notificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true,
            language = prefs[Keys.LANGUAGE] ?: Constants.Language.SYSTEM_DEFAULT,
            themeMode = prefs[Keys.THEME_MODE] ?: Constants.ThemeMode.SYSTEM,
            sensitivity = prefs[Keys.SENSITIVITY] ?: Constants.Sensitivity.STANDARD,
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            experimentalAiDetectionEnabled = prefs[Keys.EXPERIMENTAL_AI] ?: false
        )
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SERVICE_ENABLED] = enabled }
    }

    suspend fun setMonitoringPaused(paused: Boolean) {
        context.dataStore.edit { it[Keys.MONITORING_PAUSED] = paused }
    }

    suspend fun setClickDelay(delayMs: Long) {
        val clamped = delayMs.coerceIn(Constants.MIN_CLICK_DELAY_MS, Constants.MAX_CLICK_DELAY_MS)
        context.dataStore.edit { it[Keys.CLICK_DELAY] = clamped }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setSensitivity(sensitivity: String) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = sensitivity }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[Keys.START_ON_BOOT] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setExperimentalAiDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EXPERIMENTAL_AI] = enabled }
    }
}
