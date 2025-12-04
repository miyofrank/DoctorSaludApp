package com.miyo.doctorsaludapp.domain.usecase.patient

import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow

class GetPatientsUseCase(private val repo: PatientRepository) {
    operator fun invoke(): Flow<List<Patient>> = repo.getPatientsStream()
}
