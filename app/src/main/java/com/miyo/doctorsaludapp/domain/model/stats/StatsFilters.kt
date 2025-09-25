package com.miyo.doctorsaludapp.domain.model.stats

import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StatsFilters(
    val start: Date,
    val end: Date,
    /** "day" | "month" | "year" */
    val granularity: String = "month"
)

/** Filtros por defecto: últimos [monthsBack] meses, granularidad mensual */
fun defaultStatsFilters(monthsBack: Int = 6): StatsFilters {
    val calEnd = Calendar.getInstance()
    calEnd.time = Date()
    calEnd.set(Calendar.HOUR_OF_DAY, 23)
    calEnd.set(Calendar.MINUTE, 59)
    calEnd.set(Calendar.SECOND, 59)
    calEnd.set(Calendar.MILLISECOND, 999)
    val end = calEnd.time

    val calStart = Calendar.getInstance()
    calStart.time = end
    calStart.add(Calendar.MONTH, -monthsBack)
    calStart.set(Calendar.DAY_OF_MONTH, 1)
    calStart.set(Calendar.HOUR_OF_DAY, 0)
    calStart.set(Calendar.MINUTE, 0)
    calStart.set(Calendar.SECOND, 0)
    calStart.set(Calendar.MILLISECOND, 0)
    val start = calStart.time

    return StatsFilters(start = start, end = end, granularity = "month")
}
