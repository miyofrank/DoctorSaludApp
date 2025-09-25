package com.miyo.doctorsaludapp.domain.usecase.stats

import com.miyo.doctorsaludapp.data.repository.StatsRepository
import com.miyo.doctorsaludapp.domain.model.stats.StatsFilters
import com.miyo.doctorsaludapp.domain.model.stats.StatsResult

class GetStatsUseCase(
    private val repo: StatsRepository = StatsRepository()
) {
    suspend operator fun invoke(filters: StatsFilters): StatsResult = repo.fetch(filters)
}
