package com.autoadskipper.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoadskipper.data.model.HealthCheckItem
import com.autoadskipper.data.model.SystemHealthState
import com.autoadskipper.utils.AccessibilityUtils
import com.autoadskipper.utils.SystemHealthUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SystemHealthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SystemHealthState())
    val state: StateFlow<SystemHealthState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = SystemHealthUtils.getSystemHealthState(context)
    }

    fun healthChecks(state: SystemHealthState): List<HealthCheckItem> = listOf(
        HealthCheckItem(
            title = "Accessibility Service",
            description = "Required — lets Auto Ad Skipper see and tap skip buttons.",
            isHealthy = state.accessibilityGranted,
            actionLabel = if (!state.accessibilityGranted) "Enable" else null
        ),
        HealthCheckItem(
            title = "Battery Optimization",
            description = "Recommended — prevents Android from pausing monitoring in the background.",
            isHealthy = state.batteryOptimizationIgnored,
            actionLabel = if (!state.batteryOptimizationIgnored) "Fix" else null
        ),
        HealthCheckItem(
            title = "Notifications",
            description = "Recommended — needed to show skip alerts and the monitoring status.",
            isHealthy = state.notificationsEnabled,
            actionLabel = if (!state.notificationsEnabled) "Enable" else null
        ),
        HealthCheckItem(
            title = "Display Over Other Apps",
            description = "Optional — not required for current features, reserved for possible future overlays.",
            isHealthy = state.overlayPermissionGranted,
            actionLabel = if (!state.overlayPermissionGranted) "Grant" else null
        )
    )

    fun onAccessibilityAction() = AccessibilityUtils.openAccessibilitySettings(context)
    fun onBatteryAction() = SystemHealthUtils.openBatteryOptimizationSettings(context)
    fun onNotificationAction() = SystemHealthUtils.openNotificationSettings(context)
    fun onOverlayAction() = SystemHealthUtils.openOverlaySettings(context)
}
