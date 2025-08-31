package com.miyo.doctorsaludapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.domain.model.Paciente
import kotlinx.coroutines.tasks.await

class RemoteDataSourceImpl : RemoteDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val pacientesCollection = db.collection("pacientes")

    override suspend fun getPacientes(): List<Paciente> {
        return try {
            val snapshot = pacientesCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(Paciente::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addPaciente(paciente: Paciente) {
        try {
            pacientesCollection.add(paciente).await()
        } catch (e: Exception) {
            // Manejar el error apropiadamente
        }
    }

    override suspend fun updatePaciente(paciente: Paciente) {
        try {
            val document = pacientesCollection.whereEqualTo("id", paciente.id).get().await().documents.firstOrNull()
            document?.reference?.set(paciente)?.await()
        } catch (e: Exception) {
            // Manejar el error apropiadamente
        }
    }

    override suspend fun deletePaciente(paciente: Paciente) {
        try {
            val document = pacientesCollection.whereEqualTo("id", paciente.id).get().await().documents.firstOrNull()
            document?.reference?.delete()?.await()
        } catch (e: Exception) {
            // Manejar el error apropiadamente
        }
    }
}