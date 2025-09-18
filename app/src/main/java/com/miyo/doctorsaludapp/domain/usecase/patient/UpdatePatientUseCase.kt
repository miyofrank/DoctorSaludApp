package com.miyo.doctorsaludapp.domain.usecase.patient

import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.repository.PatientRepository

class UpdatePatientUseCase(private val repo: PatientRepository) {
    suspend operator fun invoke(patient: Patient) = repo.updatePatient(patient)
}
