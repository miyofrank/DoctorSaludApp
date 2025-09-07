package com.miyo.doctorsaludapp.domain.usecase.patient

import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.repository.PatientRepository

class SetPatientUseCase(private val repo: PatientRepository) {
    suspend operator fun invoke(id: String, patient: Patient) = repo.setPatient(id, patient)
}
