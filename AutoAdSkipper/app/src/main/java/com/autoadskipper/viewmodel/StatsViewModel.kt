package com.autoadskipper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoadskipper.data.StatisticsRepository
import com.autoadskipper.data.model.SkipStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    val statistics: StateFlow<SkipStatistics> = statisticsRepository.statisticsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SkipStatistics()
    )

    fun mostFrequentApps(stats: SkipStatistics, limit: Int = 5): List<Pair<String, Int>> =
        stats.appFrequency.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }

    fun resetStatistics() {
        viewModelScope.launch { statisticsRepository.resetAllStatistics() }
    }
}
