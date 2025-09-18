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
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "ritmo" to ritmo,
        "fc_bpm" to fc_bpm,
        "pr_ms" to pr_ms,
        "qrs_ms" to qrs_ms,
        "qt_ms" to qt_ms,
        "qtc_ms" to qtc_ms,
        "precisionIA" to precisionIA,
        "nivelRiesgo" to nivelRiesgo,
        "interpretacion" to interpretacion,
        "recomendacion" to recomendacion
    )
}
