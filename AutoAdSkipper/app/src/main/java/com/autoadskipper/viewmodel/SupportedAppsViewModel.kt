package com.autoadskipper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoadskipper.data.AppListRepository
import com.autoadskipper.data.model.MonitoredAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportedAppsViewModel @Inject constructor(
    private val appListRepository: AppListRepository
) : ViewModel() {

    val apps: StateFlow<List<MonitoredAppInfo>> = appListRepository.monitoredAppsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setMonitoringEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { appListRepository.setMonitoringEnabled(packageName, enabled) }
    }

    fun enableAllMonitoring() {
        viewModelScope.launch { appListRepository.enableAllMonitoring() }
    }

    fun disableAllMonitoring() {
        viewModelScope.launch { appListRepository.disableAllMonitoring() }
    }
}
