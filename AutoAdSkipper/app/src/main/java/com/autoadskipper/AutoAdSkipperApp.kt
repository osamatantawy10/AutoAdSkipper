package com.autoadskipper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.autoadskipper.utils.Constants
import com.autoadskipper.utils.LocaleUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutoAdSkipperApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleUtils.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val skipChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            val monitoringChannel = NotificationChannel(
                Constants.MONITORING_NOTIFICATION_CHANNEL_ID,
                getString(R.string.monitoring_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.monitoring_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(skipChannel)
            manager?.createNotificationChannel(monitoringChannel)
        }
    }
}
