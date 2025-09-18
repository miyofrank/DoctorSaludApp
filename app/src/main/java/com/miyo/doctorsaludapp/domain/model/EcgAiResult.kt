package com.miyo.doctorsaludapp.domain.model

data class EcgAiResult(
    val ritmo: String,
    val fc_bpm: Int?,
    val pr_ms: Double?,
    val qrs_ms: Double?,
    val qt_ms: Double?,
    val qtc_ms: Double?,
    val precisionIA: Double?,
    val nivelRiesgo: String,
    val interpretacion: String,
    val recomendacion: String
)
