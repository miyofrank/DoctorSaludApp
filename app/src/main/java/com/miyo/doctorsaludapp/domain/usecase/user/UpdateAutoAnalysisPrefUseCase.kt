package com.miyo.doctorsaludapp.domain.usecase.user

import com.miyo.doctorsaludapp.data.repository.FirestoreUserRepository

class UpdateAutoAnalysisPrefUseCase(private val repo: FirestoreUserRepository) {
    suspend operator fun invoke(uid: String, enabled: Boolean) = repo.updateAutoAnalysis(uid, enabled)
}
