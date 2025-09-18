package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyo.doctorsaludapp.domain.model.stats.StatsSummary
import com.miyo.doctorsaludapp.domain.usecase.stats.GetStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val loading: Boolean = true,
    val data: StatsSummary? = null,
    val error: String? = null
)

class StatsViewModel(
    private val getStats: GetStatsUseCase = GetStatsUseCase()
) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state

    fun load(lastMonths: Int = 12) {
        _state.value = StatsUiState(loading = true)
        viewModelScope.launch {
            try {
                val res = getStats(lastMonths)
                _state.value = StatsUiState(loading = false, data = res)
            } catch (e: Exception) {
                _state.value = StatsUiState(loading = false, error = e.message ?: "Error desconocido")
            }
        }
    }
}
