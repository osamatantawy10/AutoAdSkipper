package com.autoadskipper.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Real per-app language switching.
 *
 * The previous approach (AppCompatDelegate.setApplicationLocales()) relies
 * on the calling Activity extending AppCompatActivity to reliably apply the
 * locale pre-Android 13 — this app's activities are plain ComponentActivity
 * (Compose), so on API < 33 the locale change was silently not taking
 * effect, which is exactly the "language switching doesn't work" symptom.
 *
 * This implementation instead:
 *  1. Persists the chosen language synchronously (a small dedicated
 *     SharedPreferences file, not DataStore — attachBaseContext runs before
 *     DataStore's async Flow could deliver a value, so it must be readable
 *     synchronously and immediately at process/activity start).
 *  2. Wraps the base Context's Configuration with that locale in both
 *     Application.attachBaseContext (so services/receivers/notifications
 *     built off the app context are correct too) and Activity.attachBaseContext
 *     (so Compose's resource resolution is correct).
 *  3. Relies on the caller (SettingsViewModel) to trigger an Activity
 *     recreate() after changing the language, since a Configuration change
 *     made after an Activity/Application is already running doesn't
 *     retroactively re-resolve already-created Context resources — the
 *     process must re-read them with the new wrapped context.
 *
 * This works identically on every supported API level (26+), with no
 * dependency on AppCompatActivity or the system LocaleManager backport.
 */
object LocaleUtils {

    private const val PREFS_NAME = "auto_ad_skipper_locale_prefs"
    private const val KEY_LANGUAGE = "language"

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, Constants.Language.SYSTEM_DEFAULT)
            ?: Constants.Language.SYSTEM_DEFAULT
    }

    fun saveLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    /**
     * Wraps [base] with a Configuration set to the saved language, or
     * returns [base] unchanged if the preference is "system default".
     * Call from attachBaseContext in both Application and every Activity.
     */
    fun wrapContext(base: Context): Context {
        val languageCode = getSavedLanguage(base)
        if (languageCode == Constants.Language.SYSTEM_DEFAULT) return base

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return base.createConfigurationContext(config)
    }
}
