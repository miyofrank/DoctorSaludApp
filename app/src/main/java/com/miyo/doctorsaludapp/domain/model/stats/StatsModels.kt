package com.miyo.doctorsaludapp.domain.model.stats

data class RiskDistribution(
    val bajo: Int = 0,
    val moderado: Int = 0,
    val alto: Int = 0,
    val critico: Int = 0
) { val total: Int get() = bajo + moderado + alto + critico }

data class MonthTrend(
    val year: Int,
    val month: Int,             // 1..12
    val count: Int,
    val avgIaSeconds: Double?,  // promedio (analyzedAt - createdAt) en s
    val avgPrecision: Double?   // 0..100
) {
    val monthLabel: String get() = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[month-1]
}

data class StatsSummary(
    val totalPacientes: Int,
    val avgPrecisionGlobal: Double?, // 0..100
    val avgIaSeconds: Double?,       // s
    val avgManualMinutes: Double = 15.5,
    val savedMinutes: Double? = null,
    val risk: RiskDistribution,
    val monthly: List<MonthTrend>
)
