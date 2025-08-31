package com.miyo.doctorsaludapp.domain.model

data class Paciente(
    var id: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val direccion: String = "",
    val edad: Int = 0,
    val fecha: String = "",
    val genero: String = "",
    val telefono: Int = 0,
    val fotoUrl: String = ""
)