package com.miyo.doctorsaludapp.domain.model

import androidx.annotation.Keep
import java.io.Serializable
import java.util.Date

@Keep
data class Patient(

    // Identificador de documento en Firestore
    var id: String? = null,

    // Identidad
    var nombreCompleto: String? = null,  // preferido
    var nombre: String? = null,          // opcional si usas nombre+apellidos por separado
    var dni: String? = null,
    var edad: Int? = null,
    var sexo: String? = null,

    // Signos básicos opcionales
    var alturaCm: Int? = null,
    var pesoKg: Int? = null,

    // Cirugía programada
    var cirugia: String? = null,
    var fechaCirugia: Date? = null,
    var cirujano: String? = null,
    var duracionEstimadaMin: Int? = null,

    // Estado/Riesgo (semáforo)
    var estado: String? = null,          // "En evaluación" | "Apto" | "No apto" | etc.
    var riesgo: String? = null,          // "Bajo" | "Moderado" | "Alto"
    var riesgoPct: Int? = null,          // porcentaje de riesgo si aplica

    // ECG
    var ecgId: String? = null,           // id o referencia al estudio ECG

    // Antecedentes / Medicación (opcional)
    var antecedentes: List<String>? = null,
    var medicamentosActuales: List<String>? = null,

    // Metadatos
    var createdAt: Date? = null,
    var updatedAt: Date? = null

) : Serializable {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(
        id = null,
        nombreCompleto = null,
        nombre = null,
        dni = null,
        edad = null,
        sexo = null,
        alturaCm = null,
        pesoKg = null,
        cirugia = null,
        fechaCirugia = null,
        cirujano = null,
        duracionEstimadaMin = null,
        estado = null,
        riesgo = null,
        riesgoPct = null,
        ecgId = null,
        antecedentes = null,
        medicamentosActuales = null,
        createdAt = null,
        updatedAt = null
    )
}
