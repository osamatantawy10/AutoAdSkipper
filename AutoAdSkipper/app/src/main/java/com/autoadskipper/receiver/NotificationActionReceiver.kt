package com.autoadskipper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoadskipper.data.DataStoreManager
import com.autoadskipper.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the Pause/Resume action buttons on the persistent monitoring
 * notification. Simply flips a DataStore flag; the accessibility service
 * observes that flag reactively (see AdSkipAccessibilityService) and both
 * stops acting on ads and refreshes the notification text accordingly.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var dataStoreManager: DataStoreManager

    override fun onReceive(context: Context, intent: Intent) {
        val paused = when (intent.action) {
            Constants.ACTION_PAUSE_MONITORING -> true
            Constants.ACTION_RESUME_MONITORING -> false
            else -> return
        }
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.setMonitoringPaused(paused)
        }
    }
}
