package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miyo.doctorsaludapp.domain.usecase.LoginUseCase
import com.miyo.doctorsaludapp.domain.usecase.RegisterUseCase

class AuthViewModelFactory(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(loginUseCase, registerUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}