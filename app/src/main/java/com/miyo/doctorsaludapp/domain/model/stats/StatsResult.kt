package com.miyo.doctorsaludapp.domain.model.stats

data class RiskBucket(
    val bajo: Int = 0,
    val moderado: Int = 0,
    val alto: Int = 0,
    val critico: Int = 0
)

data class MonthlyPoint(
    val year: Int,
    val month: Int,          // 1..12
    val day: Int? = null,    // sólo si granularity="day"
    val monthLabel: String,  // "Ene 2025", "2025-09-03", etc.
    val count: Int = 0,
    val avgPrecision: Double? = null, // 96..100 (ya normalizada)
    val avgIaSeconds: Double? = null
)

data class StatsResult(
    val totalPacientes: Int = 0,
    val avgPrecisionGlobal: Double? = null,  // 96..100
    val avgIaSeconds: Double? = null,        // segundos
    val avgManualMinutes: Double = 15.5,     // fijo según tu tesis
    val savedMinutes: Double? = null,        // ahorro promedio
    val risk: RiskBucket = RiskBucket(),
    val monthly: List<MonthlyPoint> = emptyList()
)
