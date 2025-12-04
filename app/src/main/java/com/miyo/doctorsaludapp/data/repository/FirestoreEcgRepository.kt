package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis

/**
 * Repo para subcolección: pacientes/{patientId}/ecgs/{ecgId}
 * - markAnalysisStart(): marca INICIO (analysis.createdAt) en servidor
 * - saveAnalysis(): guarda el resultado, marca FIN (analysis.analyzedAt) y durationMs
 * - upsertMeta(): crea/actualiza metadatos del ECG
 */
class FirestoreEcgRepository(
    private val db: FirebaseFirestore,
    private val patientsCollection: String = "pacientes"
) {
    private fun ecgsRef(patientId: String) =
        db.collection(patientsCollection).document(patientId).collection("ecgs")

    /** Crea/actualiza la metadata del ECG (cuando subes a Storage). */
    suspend fun upsertMeta(
        patientId: String,
        ecgId: String,
        url: String,
        mime: String?,
        storagePath: String
    ) {
        val docRef = ecgsRef(patientId).document(ecgId)
        val data = hashMapOf<String, Any?>(
            "id" to ecgId,
            "patientId" to patientId,
            "url" to url,
            "mime" to mime,
            "storagePath" to storagePath,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val snap = docRef.get().await()
        if (!snap.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        docRef.set(data, SetOptions.merge()).await()
    }

    /** Marca el INICIO del análisis en servidor (timestamps). */
    suspend fun markAnalysisStart(patientId: String, ecgId: String) {
        val doc = ecgsRef(patientId).document(ecgId)
        val data = mapOf(
            "analyzed" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
            "analysis" to mapOf(
                "createdAt" to FieldValue.serverTimestamp(), // inicio
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        doc.set(data, SetOptions.merge()).await()
    }

    /**
     * Guarda el resultado y marca FIN del análisis.
     * durationMs: duración exacta medida en cliente (opcional pero recomendable).
     */
    suspend fun saveAnalysis(
        patientId: String,
        ecgId: String,
        analysis: EcgAnalysis,
        durationMs: Long? = null
    ) {
        val docRef = ecgsRef(patientId).document(ecgId)

        val analysisData = hashMapOf<String, Any?>(
            "source" to analysis.source,
            "ritmo" to analysis.ritmo,
            "fc_bpm" to analysis.fc_bpm,
            "pr_ms" to analysis.pr_ms,
            "qrs_ms" to analysis.qrs_ms,
            "qt_ms" to analysis.qt_ms,
            "qtc_ms" to analysis.qtc_ms,
            "precisionIA" to analysis.precisionIA,
            "nivelRiesgo" to analysis.nivelRiesgo,
            "interpretacion" to analysis.interpretacion,
            "recomendacion" to analysis.recomendacion,
            "updatedAt" to FieldValue.serverTimestamp(),
            "analyzedAt" to FieldValue.serverTimestamp() // fin
        ).apply {
            if (durationMs != null) put("durationMs", durationMs)
            // OJO: NO seteamos createdAt aquí; se hizo en markAnalysisStart()
        }

        val root = hashMapOf<String, Any?>(
            "analysis" to analysisData,
            "analyzed" to true,
            "analyzedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(root, SetOptions.merge()).await()
    }
}

