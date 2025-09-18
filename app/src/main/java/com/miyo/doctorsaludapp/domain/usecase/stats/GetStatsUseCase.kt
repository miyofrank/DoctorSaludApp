package com.miyo.doctorsaludapp.domain.usecase.stats

import com.miyo.doctorsaludapp.data.repository.StatsRepository
import com.miyo.doctorsaludapp.domain.model.stats.StatsSummary

class GetStatsUseCase(
    private val repo: StatsRepository = StatsRepository()
) {
    suspend operator fun invoke(lastMonths: Int = 6): StatsSummary = repo.fetchStats(lastMonths)
}
