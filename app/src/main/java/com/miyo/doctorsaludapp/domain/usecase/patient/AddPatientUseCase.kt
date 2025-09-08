package com.miyo.doctorsaludapp.domain.usecase.patient

import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.repository.PatientRepository

class AddPatientUseCase(private val repo: PatientRepository) {
    suspend operator fun invoke(patient: Patient): String = repo.addPatient(patient)
}
