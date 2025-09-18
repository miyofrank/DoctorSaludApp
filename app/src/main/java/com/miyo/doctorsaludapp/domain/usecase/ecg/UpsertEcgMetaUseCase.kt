package com.miyo.doctorsaludapp.domain.usecase.ecg

import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.FirestoreEcgRepository

class UpsertEcgMetaUseCase(
    private val repo: FirestoreEcgRepository = FirestoreEcgRepository(FirebaseFirestore.getInstance(), "pacientes")
) {
    suspend operator fun invoke(
        patientId: String,
        ecgId: String,
        url: String,
        mime: String?,
        storagePath: String
    ) = repo.upsertMeta(patientId, ecgId, url, mime, storagePath)
}
