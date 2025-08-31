package com.miyo.doctorsaludapp.domain.usecase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.miyo.doctorsaludapp.domain.model.User
import com.miyo.doctorsaludapp.domain.repository.LoginRepository

class LoginUseCase(private val repository: LoginRepository) {
    suspend fun execute(email: String, password: String): User {
        return repository.login(email, password)
    }
}

