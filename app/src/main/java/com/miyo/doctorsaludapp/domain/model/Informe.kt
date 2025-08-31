package com.miyo.doctorsaludapp.domain.model

data class Informe(
    var pacienteId: String = "",
    var diagnostico: String = "",
    var edad: Int = 0,
    var genero: String = "",
    var dolorPecho: Int = 0,
    var presionArterial: Float = 0f,
    var colesterol: Float = 0f,
    var azucarEnSangre: Int = 0,
    var restEcg: Int = 0,
    var frecuenciaCardiaca: Float = 0f,
    var anginaEjercicio: Int = 0,
    var oldpeak: Float = 0f,
    var pendiente: Int = 0,
    var vasosColoreados: Int = 0,
    var talasemia: Int = 0,
    var apto: Boolean = false // Nueva propiedad para determinar si el paciente es apto o no
)

