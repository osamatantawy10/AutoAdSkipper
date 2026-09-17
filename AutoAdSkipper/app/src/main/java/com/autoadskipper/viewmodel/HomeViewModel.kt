package com.autoadskipper.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoadskipper.data.DataStoreManager
import com.autoadskipper.data.StatisticsRepository
import com.autoadskipper.data.model.ServiceStatus
import com.autoadskipper.data.model.SkipStatistics
import com.autoadskipper.utils.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val serviceStatus: ServiceStatus = ServiceStatus.DISABLED,
    val autoSkipEnabled: Boolean = false,
    val monitoringPaused: Boolean = false,
    val statistics: SkipStatistics = SkipStatistics()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                dataStoreManager.settingsFlow,
                statisticsRepository.statisticsFlow
            ) { settings, stats -> settings to stats }
                .collect { (settings, stats) ->
                    val permissionGranted = AccessibilityUtils.isAccessibilityPermissionGranted(context)
                    val status = computeStatus(permissionGranted, settings.serviceEnabled, settings.monitoringPaused)
                    _uiState.value = HomeUiState(
                        serviceStatus = status,
                        autoSkipEnabled = settings.serviceEnabled,
                        monitoringPaused = settings.monitoringPaused,
                        statistics = stats
                    )
                }
        }
    }

    private fun computeStatus(permissionGranted: Boolean, enabled: Boolean, paused: Boolean): ServiceStatus = when {
        !enabled -> ServiceStatus.DISABLED
        enabled && !permissionGranted -> ServiceStatus.ENABLED_BUT_PERMISSION_MISSING
        enabled && paused -> ServiceStatus.ENABLED_BUT_PAUSED
        else -> ServiceStatus.ENABLED_AND_RUNNING
    }

    /** Re-checks OS permission state, e.g. after returning from Settings. */
    fun refreshPermissionStatus() {
        val permissionGranted = AccessibilityUtils.isAccessibilityPermissionGranted(context)
        val current = _uiState.value
        _uiState.value = current.copy(
            serviceStatus = computeStatus(permissionGranted, current.autoSkipEnabled, current.monitoringPaused)
        )
    }

    fun setAutoSkipEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setServiceEnabled(enabled) }
    }

    fun setMonitoringPaused(paused: Boolean) {
        viewModelScope.launch { dataStoreManager.setMonitoringPaused(paused) }
    }

    fun openAccessibilitySettings() {
        AccessibilityUtils.openAccessibilitySettings(context)
    }
}
