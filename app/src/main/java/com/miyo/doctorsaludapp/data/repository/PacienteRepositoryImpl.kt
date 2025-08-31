package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.local.data.LocalDataSource
import com.miyo.doctorsaludapp.data.remote.RemoteDataSource
import com.miyo.doctorsaludapp.domain.model.Paciente
import com.miyo.doctorsaludapp.domain.repository.PacienteRepository
import kotlinx.coroutines.tasks.await

class PacienteRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : PacienteRepository {

    override suspend fun listarPacientes(): List<Paciente> {
        return try {
            val snapshot = firestore.collection("pacientes").get().await()
            snapshot.toObjects(Paciente::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun registrarPaciente(paciente: Paciente) {
        remoteDataSource.addPaciente(paciente)
        // Opcional: almacenar en localDataSource
        // localDataSource.addPaciente(paciente)
    }

    override suspend fun editarPaciente(paciente: Paciente) {
        remoteDataSource.updatePaciente(paciente)
        // Opcional: actualizar en localDataSource
        // localDataSource.updatePaciente(paciente)
    }

    override suspend fun eliminarPaciente(paciente: Paciente) {
        remoteDataSource.deletePaciente(paciente)
        // Opcional: eliminar en localDataSource
        // localDataSource.deletePaciente(paciente)
    }
}
