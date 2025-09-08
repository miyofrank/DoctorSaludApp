package com.miyo.doctorsaludapp.domain.usecase.patient

import com.miyo.doctorsaludapp.domain.repository.PatientRepository

class DeletePatientUseCase(private val repo: PatientRepository) {
    suspend operator fun invoke(id: String) = repo.deletePatient(id)
}
