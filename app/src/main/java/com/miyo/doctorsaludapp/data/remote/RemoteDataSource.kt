package com.miyo.doctorsaludapp.data.remote

import com.miyo.doctorsaludapp.domain.model.Paciente

interface RemoteDataSource {
    suspend fun getPacientes(): List<Paciente>
    suspend fun addPaciente(paciente: Paciente)
    suspend fun updatePaciente(paciente: Paciente)
    suspend fun deletePaciente(paciente: Paciente)
}