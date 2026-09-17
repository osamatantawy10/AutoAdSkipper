package com.autoadskipper.utils

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.autoadskipper.accessibility.AdSkipAccessibilityService

/**
 * Reads the OS-level "is this accessibility service enabled by the user" flag
 * from Settings.Secure. This is distinct from the in-app "auto skip" toggle
 * stored in DataStore — a user can grant the permission once, then flip the
 * in-app switch on/off freely without re-visiting system settings each time.
 */
object AccessibilityUtils {

    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/${AdSkipAccessibilityService::class.java.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServicesSetting)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
