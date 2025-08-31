package com.miyo.doctorsaludapp.data.remote

interface ApiService {
    suspend fun getPacientes(): List<PacienteDto>
    suspend fun addPaciente(paciente: PacienteDto)
    suspend fun updatePaciente(paciente: PacienteDto)
    suspend fun deletePaciente(pacienteId: String)
}