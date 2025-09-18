package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis
import com.miyo.doctorsaludapp.domain.model.EcgRecord
import kotlinx.coroutines.tasks.await

/**
 * Estructura:
 *   pacientes/{patientId}/ecgs/{ecgId}
 */
class FirestoreEcgRepository(
    private val db: FirebaseFirestore,
    private val patientsCollection: String = "pacientes"
) {

    private fun ecgsRef(patientId: String) =
        db.collection(patientsCollection).document(patientId).collection("ecgs")

    suspend fun get(patientId: String, ecgId: String): EcgRecord? {
        val snap = ecgsRef(patientId).document(ecgId).get().await()
        return snap.toObject(EcgRecord::class.java)?.copy(id = ecgId, patientId = patientId)
    }

    /**
     * Crea o actualiza un ECG con metadatos del archivo subido.
     * Usa transacción para setear createdAt solo si el doc no existe.
     */
    suspend fun upsertMeta(
        patientId: String,
        ecgId: String,
        url: String,
        mime: String?,
        storagePath: String
    ) {
        db.runTransaction { trx ->
            val docRef = ecgsRef(patientId).document(ecgId)
            val snap = trx.get(docRef)
            val data = hashMapOf<String, Any?>(
                "id" to ecgId,
                "patientId" to patientId,
                "url" to url,
                "mime" to mime,
                "storagePath" to storagePath,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                data["createdAt"] = FieldValue.serverTimestamp()
                data["analyzed"] = false
            }
            trx.set(docRef, data, SetOptions.merge())
        }.await()
    }

    /**
     * Guarda el resultado de análisis en el ECG y marca analyzed=true.
     */
    suspend fun saveAnalysis(
        patientId: String,
        ecgId: String,
        analysis: EcgAnalysis
    ) {
        val docRef = ecgsRef(patientId).document(ecgId)
        val data = hashMapOf<String, Any?>(
            "analysis" to hashMapOf(
                "source" to (analysis.source),
                "ritmo" to (analysis.ritmo),
                "fc_bpm" to (analysis.fc_bpm),
                "pr_ms" to (analysis.pr_ms),
                "qrs_ms" to (analysis.qrs_ms),
                "qt_ms" to (analysis.qt_ms),
                "qtc_ms" to (analysis.qtc_ms),
                "precisionIA" to (analysis.precisionIA),
                "nivelRiesgo" to (analysis.nivelRiesgo),
                "interpretacion" to (analysis.interpretacion),
                "recomendacion" to (analysis.recomendacion),
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp() // si no existía antes
            ),
            "analyzed" to true,
            "analyzedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data, SetOptions.merge()).await()
    }

    /**
     * Lista los ECGs de un paciente (últimos primero).
     */
    suspend fun listByPatient(patientId: String, limit: Long = 50): List<EcgRecord> {
        val q = ecgsRef(patientId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return q.documents.mapNotNull { d ->
            d.toObject(EcgRecord::class.java)?.copy(id = d.id, patientId = patientId)
        }
    }
}
