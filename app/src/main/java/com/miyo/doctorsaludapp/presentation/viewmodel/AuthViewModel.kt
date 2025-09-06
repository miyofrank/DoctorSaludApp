package com.miyo.doctorsaludapp.presentation.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.miyo.doctorsaludapp.domain.usecase.LoginUseCase
import com.miyo.doctorsaludapp.domain.usecase.RegisterUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                loginUseCase.execute(email, password)
                _user.value = auth.currentUser
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String, specialization: String, hospital: String, licencia: String) {
        viewModelScope.launch {
            try {
                registerUseCase.execute(email, password, firstName, lastName, specialization,hospital, licencia )
                _user.value = auth.currentUser
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loginWithCredential(credential: AuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _user.value = auth.currentUser
            } else {
                _error.value = task.exception?.message ?: "Authentication failed"
            }
        }.addOnFailureListener { exception ->
            _error.value = exception.message ?: "Unknown error occurred"
        }
    }

    fun notifyError(error: String) {
        _error.postValue(error)
    }
}