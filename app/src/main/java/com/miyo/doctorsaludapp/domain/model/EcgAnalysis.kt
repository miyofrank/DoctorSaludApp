package com.miyo.doctorsaludapp.domain.model

import androidx.annotation.Keep
import java.io.Serializable
import java.util.Date

@Keep
data class EcgAnalysis(
    var source: String = "gemini",
    var ritmo: String = "Desconocido",
    var fc_bpm: Int? = null,
    var pr_ms: Double? = null,
    var qrs_ms: Double? = null,
    var qt_ms: Double? = null,
    var qtc_ms: Double? = null,
    var precisionIA: Double? = null,
    var nivelRiesgo: String = "Bajo",
    var interpretacion: String = "",
    var recomendacion: String = "",

    var createdAt: Date? = null,
    var updatedAt: Date? = null
) : Serializable {
    constructor() : this(source = "gemini")
}
