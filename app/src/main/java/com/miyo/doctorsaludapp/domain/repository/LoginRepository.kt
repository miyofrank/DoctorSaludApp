package com.miyo.doctorsaludapp.domain.repository

import com.google.firebase.auth.FirebaseUser
import com.miyo.doctorsaludapp.domain.model.User

interface LoginRepository {
    suspend fun login(email: String, password: String): User
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        specialization: String,
        hospital: String,
        licencia: String
    ): FirebaseUser?
}