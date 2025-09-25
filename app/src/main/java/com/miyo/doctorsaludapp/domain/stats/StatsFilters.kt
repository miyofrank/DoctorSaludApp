package com.miyo.doctorsaludapp.domain.stats

import java.util.*

data class StatsFilters(
    val start: Date,
    val end: Date,
    val granularity: Granularity
)
