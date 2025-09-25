package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.StatsRepository
import com.miyo.doctorsaludapp.data.repository.StatsResult
import com.miyo.doctorsaludapp.domain.stats.Granularity
import com.miyo.doctorsaludapp.domain.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.common.Filters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

data class StatsUi(
    val loading: Boolean = true,
    val data: StatsResult? = null,
    val error: String? = null
)

class StatsViewModel(
    private val repo: StatsRepository = StatsRepository(FirebaseFirestore.getInstance(), "pacientes")
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUi())
    val state: StateFlow<StatsUi> = _state

    fun loadDefault() {
        val (s,e) = Filters.range(Filters.Preset.LAST_6M)
        applyFilters(StatsFilters(s, e, Granularity.MONTH))
    }

    fun applyFilters(filters: StatsFilters) {
        _state.value = StatsUi(loading = true)
        viewModelScope.launch {
            try {
                val res = repo.fetchStats(filters)
                _state.value = StatsUi(loading = false, data = res)
            } catch (e: Exception) {
                _state.value = StatsUi(loading = false, error = e.message ?: "Error")
            }
        }
    }
}
