package com.miyo.doctorsaludapp.domain.usecase

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.domain.model.Paciente

class PacienteUseCase {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun registrarPaciente(paciente: Paciente, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("pacientes").add(paciente)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun editarPaciente(paciente: Paciente, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("pacientes").document(paciente.id).set(paciente)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun eliminarPaciente(pacienteId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("pacientes").document(pacienteId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun listarPacientes(onSuccess: (List<Paciente>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("pacientes").get()
            .addOnSuccessListener { result ->
                val pacientes = result.map { document ->
                    document.toObject(Paciente::class.java).copy(id = document.id)
                }
                onSuccess(pacientes)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun cargarFotoPaciente(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val ref = storage.reference.child("pacientes/${uri.lastPathSegment}")
        ref.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                ref.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
    fun buscarPacientePorNombre(nombre: String, onSuccess: (Paciente?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("pacientes")
            .whereEqualTo("nombre", nombre)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val paciente = result.documents[0].toObject(Paciente::class.java)?.copy(id = result.documents[0].id)
                    onSuccess(paciente)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}