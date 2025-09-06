package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.auth.FirebaseUser
import com.miyo.doctorsaludapp.data.mappers.toDomain
import com.miyo.doctorsaludapp.data.remote.FirebaseService
import com.miyo.doctorsaludapp.data.remote.UserDTO
import com.miyo.doctorsaludapp.domain.model.User
import com.miyo.doctorsaludapp.domain.repository.LoginRepository


class LoginRepositoryImpl(private val firebaseService: FirebaseService) : LoginRepository {

    override suspend fun login(email: String, password: String): User {
        val result = firebaseService.login(email, password)
        val userDTO = UserDTO(result.user?.email ?: "", result.user?.uid ?: "")
        return userDTO.toDomain()
    }

    override suspend fun register(email: String, password: String, firstName: String, lastName: String, specialization: String, hospital: String ,licencia: String): FirebaseUser? {
        return firebaseService.register(email, password, firstName, lastName, specialization, hospital, licencia)
    }
}