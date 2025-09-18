package com.miyo.doctorsaludapp.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.miyo.doctorsaludapp.domain.model.Patient
import kotlinx.coroutines.tasks.await

class PatientRepository {
    private val db = Firebase.firestore
    private fun patientsCol() = db.collection("patients")
    private fun patientDoc(patientId: String) = patientsCol().document(patientId)

    suspend fun get(patientId: String): Patient? {
        val snap = patientDoc(patientId).get().await()
        return snap.toObject(Patient::class.java)?.copy(id = patientId)
    }

    /** Actualiza campos de ECG en el Patient (ecgUrl, ecgId, ecgMime) */
    suspend fun updateEcgFields(
        patientId: String,
        ecgUrl: String?,
        ecgId: String?,
        ecgMime: String?
    ) {
        val data = mapOf(
            "ecgUrl" to ecgUrl,
            "ecgId" to ecgId,
            "ecgMime" to ecgMime,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        patientDoc(patientId).update(data).await()
    }

    /** Guarda el análisis: riesgo/porcentaje + bloque ecgAnalysis (map) */
    suspend fun saveEcgAnalysis(
        patientId: String,
        analysisMap: Map<String, Any?>,
        riesgo: String?,
        riesgoPct: Int?
    ) {
        val data = mapOf(
            "riesgo" to riesgo,
            "riesgoPct" to riesgoPct,
            "ecgAnalysis" to analysisMap,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        patientDoc(patientId).update(data).await()
    }
}
