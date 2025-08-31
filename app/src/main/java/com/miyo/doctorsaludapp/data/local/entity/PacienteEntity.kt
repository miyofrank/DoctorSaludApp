package com.miyo.doctorsaludapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paciente")
data class PacienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val apellidos: String,
    val correo: String,
    val direccion: String,
    val edad: Int,
    val fecha: String,
    val foto: String,
    val genero: String,
    val nombre: String,
    val telefono: Int
)

