package com.miyo.doctorsaludapp.data.local.data

import com.miyo.doctorsaludapp.data.local.dao.UserDao
import com.miyo.doctorsaludapp.data.local.entity.UserEntity
import com.miyo.doctorsaludapp.domain.model.Paciente

interface LocalDataSource {
    fun getPacientes(): List<Paciente>
    suspend fun addPaciente(paciente: Paciente)
    suspend fun updatePaciente(paciente: Paciente)
    suspend fun deletePaciente(paciente: Paciente)
}

