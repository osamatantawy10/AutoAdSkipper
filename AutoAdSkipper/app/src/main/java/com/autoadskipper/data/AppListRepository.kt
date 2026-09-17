package com.autoadskipper.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.autoadskipper.data.model.MonitoredAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lists user-facing installed apps (icon, label, package name) via
 * PackageManager and merges in per-app monitoring on/off state from
 * [AppPreferencesRepository]. Runs the PackageManager query off the main
 * thread since it can be a few hundred milliseconds on a phone with many
 * apps installed.
 */
@Singleton
class AppListRepository @Inject constructor(
    private val context: Context,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val statisticsRepository: StatisticsRepository
) {
    private fun queryInstalledApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { isUserFacing(it) }
            .map { it.packageName to it.loadLabel(pm).toString() }
            .sortedBy { it.second.lowercase() }
    }

    private fun isUserFacing(appInfo: ApplicationInfo): Boolean {
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasLaunchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
        return hasLaunchIntent && (!isSystemApp || appInfo.packageName == "com.google.android.youtube")
    }

    val monitoredAppsFlow: Flow<List<MonitoredAppInfo>> = combine(
        appPreferencesRepository.disabledPackagesFlow,
        statisticsRepository.statisticsFlow
    ) { disabledPackages, stats ->
        withContext(Dispatchers.Default) {
            val pm = context.packageManager
            queryInstalledApps().map { (packageName, label) ->
                MonitoredAppInfo(
                    appName = label,
                    packageName = packageName,
                    icon = runCatching { pm.getApplicationIcon(packageName) }.getOrNull(),
                    isMonitoringEnabled = packageName !in disabledPackages,
                    lastDetectedActivity = if (stats.lastSkippedPackage == packageName) "Skip button detected" else null,
                    lastDetectionTime = if (stats.lastSkippedPackage == packageName) stats.lastSkippedTime else null
                )
            }
        }
    }

    suspend fun setMonitoringEnabled(packageName: String, enabled: Boolean) {
        appPreferencesRepository.setMonitoringEnabled(packageName, enabled)
    }

    suspend fun enableAllMonitoring() {
        appPreferencesRepository.enableAllMonitoring()
    }

    suspend fun disableAllMonitoring() {
        val allPackages = withContext(Dispatchers.Default) { queryInstalledApps().map { it.first } }
        appPreferencesRepository.disableAllMonitoring(allPackages)
    }
}
