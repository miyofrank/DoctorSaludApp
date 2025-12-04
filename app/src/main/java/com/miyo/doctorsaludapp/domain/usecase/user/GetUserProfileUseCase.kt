package com.miyo.doctorsaludapp.domain.usecase.user

import com.miyo.doctorsaludapp.data.repository.FirestoreUserRepository
import com.miyo.doctorsaludapp.domain.model.UserProfile

class GetUserProfileUseCase(private val repo: FirestoreUserRepository) {
    suspend operator fun invoke(uid: String): UserProfile? = repo.getById(uid)
}
