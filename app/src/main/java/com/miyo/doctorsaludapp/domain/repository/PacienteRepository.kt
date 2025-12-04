package com.miyo.doctorsaludapp.domain.repository

import com.miyo.doctorsaludapp.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    fun getPatientsStream(): Flow<List<Patient>>
    suspend fun getPatientById(id: String): Patient?
    suspend fun addPatient(patient: Patient): String
    suspend fun setPatient(id: String, patient: Patient)   // nuevo: crear/actualizar por ID conocido
    suspend fun updatePatient(patient: Patient)
    suspend fun deletePatient(id: String)
}
