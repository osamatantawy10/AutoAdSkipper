package com.autoadskipper.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoadskipper.data.DataStoreManager
import com.autoadskipper.utils.AccessibilityUtils
import com.autoadskipper.utils.SystemHealthUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    fun openAccessibilitySettings() = AccessibilityUtils.openAccessibilitySettings(context)
    fun openBatterySettings() = SystemHealthUtils.openBatteryOptimizationSettings(context)
    fun isAccessibilityGranted(): Boolean = AccessibilityUtils.isAccessibilityPermissionGranted(context)

    fun completeOnboarding(enableService: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setOnboardingCompleted(true)
            if (enableService) dataStoreManager.setServiceEnabled(true)
        }
    }
}
