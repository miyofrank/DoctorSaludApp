package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.repository.PatientRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestorePatientRepository(
    private val db: FirebaseFirestore,
    private val collection: String = "pacientes" // Cambia si tu colección se llama distinto
) : PatientRepository {

    override fun getPatientsStream(): Flow<List<Patient>> = callbackFlow {
        val reg = db.collection(collection)
            .orderBy("nombreCompleto", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { d -> d.data?.toPatient(d.id) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getPatientById(id: String): Patient? {
        val doc = db.collection(collection).document(id).get().await()
        return doc.data?.toPatient(doc.id)
    }

    override suspend fun addPatient(patient: Patient): String {
        val ref = db.collection(collection).add(patient.toMap()).await()
        return ref.id
    }

    override suspend fun updatePatient(patient: Patient) {
        require(!patient.id.isNullOrEmpty()) { "patient.id requerido" }
        db.collection(collection).document(patient.id!!).update(patient.toMap()).await()
    }

    override suspend fun deletePatient(id: String) {
        db.collection(collection).document(id).delete().await()
    }

    // ---------- helpers (ajusta claves si tu esquema es distinto) ----------
    private fun Map<String, Any?>.toPatient(id: String): Patient? = try {
        val fechaTs = this["fechaCirugia"] as? Timestamp
        val riesgoPct = (this["riesgoPct"] as? Number)?.toInt()
            ?: (this["riesgoPct"] as? String)?.toIntOrNull()
        Patient(
            id = id,
            nombreCompleto = (this["nombreCompleto"] ?: this["nombre"]) as? String,
            dni = this["dni"] as? String,
            edad = (this["edad"] as? Number)?.toInt() ?: (this["edad"] as? String)?.toIntOrNull(),
            sexo = this["sexo"] as? String,
            cirugia = this["cirugia"] as? String,
            fechaCirugia = fechaTs?.toDate(),
            estado = this["estado"] as? String,
            riesgo = this["riesgo"] as? String,
            riesgoPct = riesgoPct,
            ecgId = this["ecgId"] as? String
        )
    } catch (_: Throwable) { null }

    private fun Patient.toMap(): Map<String, Any?> = hashMapOf(
        "nombreCompleto" to (this.nombreCompleto ?: this.nombre),
        "dni" to this.dni,
        "edad" to this.edad,
        "sexo" to this.sexo,
        "cirugia" to this.cirugia,
        "fechaCirugia" to (this.fechaCirugia?.let { Timestamp(Date(it.time)) }),
        "estado" to this.estado,
        "riesgo" to this.riesgo,
        "riesgoPct" to this.riesgoPct,
        "ecgId" to this.ecgId
    )
}
