package com.miyo.doctorsaludapp.domain.model

import androidx.annotation.Keep
import java.io.Serializable
import java.util.Date

@Keep
data class Patient(
    // Meta
    var id: String? = null,
    var createdAt: Date? = null,
    var updatedAt: Date? = null,

    // Identidad
    var dni: String? = null,
    var nombres: String? = null,
    var apellidos: String? = null,
    var nombreCompleto: String? = null,
    var edad: Int? = null,
    var sexo: String? = null,
    var grupoSanguineo: String? = null,
    var alturaCm: Int? = null,
    var pesoKg: Int? = null,

    // Antecedentes
    var alergias: List<String>? = null,
    var medicamentosActuales: List<String>? = null,
    var enfermedadesCronicas: List<String>? = null,
    var cirugiasPrevias: List<String>? = null,
    var antecedentesFamiliares: List<String>? = null,

    // Cirugía programada
    var tipoCirugia: String? = null,
    var fechaCirugia: Date? = null,
    var duracionEstimadaMin: Int? = null,
    var tipoAnestesia: String? = null,
    var urgencia: String? = null,
    var cirujano: String? = null,

    // ECG (subido a Storage)
    var ecgUrl: String? = null,      // download URL
    var ecgId: String? = null,       // nombre archivo o id
    var ecgMime: String? = null,

    // Exámenes complementarios (Storage)
    var examenesTexto: String? = null,
    var examenesArchivos: List<String>? = null, // download URLs

    // Notas y evaluación
    var notas: String? = null,
    var estado: String? = null,
    var riesgo: String? = null,
    var riesgoPct: Int? = null
) : Serializable {
    constructor() : this(
        id = null, createdAt = null, updatedAt = null,
        dni = null, nombres = null, apellidos = null, nombreCompleto = null, edad = null,
        sexo = null, grupoSanguineo = null, alturaCm = null, pesoKg = null,
        alergias = null, medicamentosActuales = null, enfermedadesCronicas = null,
        cirugiasPrevias = null, antecedentesFamiliares = null,
        tipoCirugia = null, fechaCirugia = null, duracionEstimadaMin = null,
        tipoAnestesia = null, urgencia = null, cirujano = null,
        ecgUrl = null, ecgId = null, ecgMime = null,
        examenesTexto = null, examenesArchivos = null,
        notas = null, estado = null, riesgo = null, riesgoPct = null
    )
}
