package com.miyo.doctorsaludapp.domain.usecase.ecg

import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.FirestoreEcgRepository
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis

class SaveEcgAnalysisUseCase(
    private val repo: FirestoreEcgRepository = FirestoreEcgRepository(FirebaseFirestore.getInstance(), "pacientes")
) {
    suspend operator fun invoke(
        patientId: String,
        ecgId: String,
        analysis: EcgAnalysis
    ) = repo.saveAnalysis(patientId, ecgId, analysis)
}
