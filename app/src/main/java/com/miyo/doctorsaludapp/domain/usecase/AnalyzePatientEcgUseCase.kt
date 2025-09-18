package com.miyo.doctorsaludapp.domain.usecase

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import com.miyo.doctorsaludapp.data.ai.GeminiAnalyzer
import com.miyo.doctorsaludapp.data.firestore.PatientRepository
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.util.MimeSniffer

class AnalyzePatientEcgUseCase(
    private val patients: PatientRepository = PatientRepository()
) {
    /**
     * Descarga los bytes del ECG (ecgUrl o ecgId), llama Gemini y guarda el resultado en Patient.
     * Retorna el EcgAnalysis calculado.
     */
    suspend operator fun invoke(context: Context, patientId: String): EcgAnalysis {
        val p: Patient = patients.get(patientId)
            ?: error("Paciente no existe ($patientId).")

        val bytes: ByteArray = when {
            !p.ecgUrl.isNullOrBlank() -> {
                val ref = Firebase.storage.getReferenceFromUrl(p.ecgUrl!!)
                ref.getBytes(10L * 1024L * 1024L).await()
            }
            !p.ecgId.isNullOrBlank() -> {
                val ref = Firebase.storage.reference.child("ecgs/$patientId/${p.ecgId}")
                ref.getBytes(10L * 1024L * 1024L).await()
            }
            else -> error("Paciente sin ECG. Sube o toma una imagen primero.")
        }
        val mime = p.ecgMime ?: MimeSniffer.sniff(bytes)

        val ai = GeminiAnalyzer.analyze(context, bytes, mime)

        val riesgoPct = when (ai.nivelRiesgo.lowercase()) {
            "alto" -> 85
            "moderado" -> 55
            else -> 15
        }
        val analysis = EcgAnalysis(
            ritmo = ai.ritmo,
            fc_bpm = ai.fc_bpm,
            pr_ms = ai.pr_ms,
            qrs_ms = ai.qrs_ms,
            qt_ms = ai.qt_ms,
            qtc_ms = ai.qtc_ms,
            precisionIA = ai.precisionIA,
            nivelRiesgo = ai.nivelRiesgo,
            interpretacion = ai.interpretacion,
            recomendacion = ai.recomendacion,
            updatedAt = System.currentTimeMillis()
        )

        val map = mapOf(
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
            "updatedAt" to analysis.updatedAt
        )

        patients.saveEcgAnalysis(
            patientId = patientId,
            analysisMap = map,
            riesgo = analysis.nivelRiesgo,
            riesgoPct = riesgoPct
        )
        return analysis
    }
}
