package com.autoadskipper.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoadskipper.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefsDataStore by preferencesDataStore(name = "auto_ad_skipper_app_prefs")

/**
 * Tracks which installed apps the user has explicitly turned monitoring OFF
 * for. Everything is monitored by default (empty set = all enabled); apps are
 * only ever added here when the user flips a toggle off on the Supported
 * Apps screen.
 */
@Singleton
class AppPreferencesRepository @Inject constructor(
    private val context: Context
) {
    private val key = stringSetPreferencesKey(Constants.KEY_DISABLED_PACKAGES)

    val disabledPackagesFlow: Flow<Set<String>> = context.appPrefsDataStore.data.map { prefs ->
        prefs[key] ?: emptySet()
    }

    suspend fun setMonitoringEnabled(packageName: String, enabled: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            val current = (prefs[key] ?: emptySet()).toMutableSet()
            if (enabled) current.remove(packageName) else current.add(packageName)
            prefs[key] = current
        }
    }

    /** "Monitor all applications" — clears every per-app exclusion. */
    suspend fun enableAllMonitoring() {
        context.appPrefsDataStore.edit { prefs -> prefs[key] = emptySet() }
    }

    /** Bulk-exclude every currently listed package (e.g. "Disable All"). */
    suspend fun disableAllMonitoring(packageNames: List<String>) {
        context.appPrefsDataStore.edit { prefs -> prefs[key] = packageNames.toSet() }
    }
}
