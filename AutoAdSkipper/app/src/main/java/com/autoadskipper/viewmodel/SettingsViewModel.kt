package com.autoadskipper.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoadskipper.data.DataStoreManager
import com.autoadskipper.data.model.AppSettings
import com.autoadskipper.utils.LocaleUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = dataStoreManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setAutoSkipEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setServiceEnabled(enabled) }
    }

    fun setClickDelay(delayMs: Long) {
        viewModelScope.launch { dataStoreManager.setClickDelay(delayMs) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setVibrationEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setSoundEnabled(enabled) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setNotificationEnabled(enabled) }
    }

    fun setLanguage(language: String) {
        LocaleUtils.saveLanguage(appContext, language)
        viewModelScope.launch { dataStoreManager.setLanguage(language) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { dataStoreManager.setThemeMode(mode) }
    }

    fun setSensitivity(sensitivity: String) {
        viewModelScope.launch { dataStoreManager.setSensitivity(sensitivity) }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setStartOnBoot(enabled) }
    }

    fun setExperimentalAiDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setExperimentalAiDetectionEnabled(enabled) }
    }
}
