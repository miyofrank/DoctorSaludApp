package com.miyo.doctorsaludapp.presentation.common

import java.util.*

object Filters {
    enum class Preset(val label: String) {
        LAST_7D("Últimos 7 días"),
        LAST_30D("Últimos 30 días"),
        LAST_6M("Últimos 6 meses"),
        LAST_12M("Últimos 12 meses"),
        ALL("Todo")
    }

    fun range(p: Preset): Pair<Date, Date> {
        val end = Date()
        val cal = Calendar.getInstance().apply { time = end }
        when (p) {
            Preset.LAST_7D  -> cal.add(Calendar.DAY_OF_YEAR, -7)
            Preset.LAST_30D -> cal.add(Calendar.DAY_OF_YEAR, -30)
            Preset.LAST_6M  -> cal.add(Calendar.MONTH, -6)
            Preset.LAST_12M -> cal.add(Calendar.MONTH, -12)
            Preset.ALL      -> cal.add(Calendar.YEAR, -20)
        }
        return Pair(cal.time, end)
    }
}
