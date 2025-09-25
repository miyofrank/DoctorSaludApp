package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyo.doctorsaludapp.data.repository.CollectionGroupIndexRequiredException
import com.miyo.doctorsaludapp.domain.model.stats.StatsFilters
import com.miyo.doctorsaludapp.domain.model.stats.StatsResult
import com.miyo.doctorsaludapp.domain.model.stats.defaultStatsFilters
import com.miyo.doctorsaludapp.domain.usecase.stats.GetStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val loading: Boolean = false,
    val filters: StatsFilters = defaultStatsFilters(6),
    val data: StatsResult? = null,
    val error: String? = null
)

class StatsViewModel(
    private val useCase: GetStatsUseCase = GetStatsUseCase()
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState(loading = true))
    val state: StateFlow<StatsUiState> = _state

    fun setFilters(f: StatsFilters) {
        _state.value = _state.value.copy(filters = f)
    }

    fun load(filters: StatsFilters? = null) {
        val f = filters ?: _state.value.filters
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val res = useCase(f)
                _state.value = _state.value.copy(loading = false, data = res, error = null)
            } catch (e: CollectionGroupIndexRequiredException) {
                _state.value = _state.value.copy(loading = false, data = e.partial, error = e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }
}
