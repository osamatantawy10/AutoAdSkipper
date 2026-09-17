package com.autoadskipper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoadskipper.data.DataStoreManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The accessibility service itself is restarted by the OS automatically once
 * granted — there's nothing to manually relaunch here. What "Start on Boot"
 * actually controls is whether monitoring resumes *active* right after a
 * reboot or comes back *paused*, for users who'd rather confirm things
 * manually before ads start getting auto-clicked again.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var dataStoreManager: DataStoreManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val settings = dataStoreManager.settingsFlow.first()
            dataStoreManager.setMonitoringPaused(!settings.startOnBoot)
        }
    }
}
