package com.miyo.doctorsaludapp.domain.model

import androidx.annotation.Keep
import java.io.Serializable
import java.util.Date

@Keep
data class EcgRecord(
    // Claves
    var id: String? = null,           // = ecgId / nombre del archivo
    var patientId: String? = null,

    // Archivo en Storage
    var storagePath: String? = null,  // p.ej. "patients/{id}/ecg/ecg_169..."
    var url: String? = null,          // download URL
    var mime: String? = null,

    // Estado
    var analyzed: Boolean? = null,

    // Tiempos
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var analyzedAt: Date? = null,

    // Resultado
    var analysis: EcgAnalysis? = null
) : Serializable {
    constructor() : this(id = null)
}
