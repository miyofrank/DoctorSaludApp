package com.miyo.doctorsaludapp.domain.repository
import com.miyo.doctorsaludapp.domain.model.Paciente

interface PacienteRepository {
    suspend fun listarPacientes(): List<Paciente>
    suspend fun registrarPaciente(paciente: Paciente)
    suspend fun editarPaciente(paciente: Paciente)
    suspend fun eliminarPaciente(paciente: Paciente)
}
