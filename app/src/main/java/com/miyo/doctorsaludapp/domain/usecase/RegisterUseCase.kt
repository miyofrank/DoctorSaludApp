package com.miyo.doctorsaludapp.domain.usecase

import com.google.firebase.auth.FirebaseUser
import com.miyo.doctorsaludapp.domain.model.User
import com.miyo.doctorsaludapp.domain.repository.LoginRepository

class RegisterUseCase(private val loginRepository: LoginRepository) {
    suspend fun execute(email: String, password: String, firstName: String, lastName: String, specialization: String, hospital: String, licencia : String): FirebaseUser? {
        return loginRepository.register(email, password, firstName, lastName, specialization,hospital ,licencia)
    }
}