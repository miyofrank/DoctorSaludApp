package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.miyo.doctorsaludapp.data.remote.FirebaseService
import com.miyo.doctorsaludapp.data.repository.LoginRepositoryImpl
import com.miyo.doctorsaludapp.databinding.ActivityLoginBinding
import com.miyo.doctorsaludapp.domain.usecase.LoginUseCase
import com.miyo.doctorsaludapp.domain.usecase.RegisterUseCase
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModel
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val loginUseCase by lazy { LoginUseCase(LoginRepositoryImpl(FirebaseService())) }
    private val registerUseCase by lazy { RegisterUseCase(LoginRepositoryImpl(FirebaseService())) }
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(loginUseCase, registerUseCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            authViewModel.login(email, password)
        }

        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        authViewModel.user.observe(this, { user ->
            user?.let {
                // Manejar el inicio de sesión exitoso
                moveToNextActivity()
            }
        })

        authViewModel.error.observe(this, { error ->
            error?.let {
                // Manejar el error
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun moveToNextActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()  // Finaliza LoginActivity para evitar volver atrás con el botón de atrás
    }
}
