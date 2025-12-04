package com.miyo.doctorsaludapp.domain.usecase

import android.net.Uri
import com.miyo.doctorsaludapp.data.firestore.PatientRepository
import com.miyo.doctorsaludapp.data.storage.ImageUploader

class UploadEcgForPatientUseCase(
    private val patients: PatientRepository = PatientRepository(),
    private val uploader: ImageUploader = ImageUploader()
) {
    /**
     * Sube el ECG a Storage y actualiza el Patient con ecgUrl/ecgId/ecgMime.
     */
    suspend operator fun invoke(patientId: String, imageUri: Uri, mime: String) {
        val (url, ecgId, contentType) = uploader.uploadEcg(patientId, imageUri, mime)
        patients.updateEcgFields(patientId, ecgUrl = url, ecgId = ecgId, ecgMime = contentType)
    }
}
