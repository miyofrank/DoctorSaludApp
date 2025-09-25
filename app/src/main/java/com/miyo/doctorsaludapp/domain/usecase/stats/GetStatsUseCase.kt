package com.miyo.doctorsaludapp.domain.usecase.stats

import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.StatsRepository
import com.miyo.doctorsaludapp.data.repository.StatsResult
import com.miyo.doctorsaludapp.domain.stats.Granularity
import com.miyo.doctorsaludapp.domain.stats.StatsFilters
import java.util.Calendar
import java.util.Date

class GetStatsUseCase(
    // StatsRepository ahora requiere db y collection
    private val repo: StatsRepository = StatsRepository(
        db = FirebaseFirestore.getInstance(),
        collection = "pacientes"
    )
) {
    /**
     * Usa filtros explícitos (start/end/granularity).
     */
    suspend operator fun invoke(filters: StatsFilters): StatsResult =
        repo.fetchStats(filters)

    /**
     * Conveniencia: últimos N meses (granularidad mensual).
     */
    suspend fun lastMonths(months: Int = 6): StatsResult {
        val end = Date()
        val cal = Calendar.getInstance().apply {
            time = end
            add(Calendar.MONTH, -months)
        }
        val start = cal.time
        val filters = StatsFilters(
            start = start,
            end = end,
            granularity = Granularity.MONTH
        )
        return repo.fetchStats(filters)
    }
}
