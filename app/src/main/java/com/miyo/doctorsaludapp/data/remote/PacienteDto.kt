package com.miyo.doctorsaludapp.data.remote

data class PacienteDto(
    val id: Int = 0,
    val apellidos: String = "",
    val correo: String = "",
    val direccion: String = "",
    val edad: Int = 0,
    val fecha: String = "",
    val foto: String = "",
    val genero: String = "",
    val nombre: String = "",
    val telefono: Int = 0
)

