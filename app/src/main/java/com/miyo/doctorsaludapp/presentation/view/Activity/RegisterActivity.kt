package com.miyo.doctorsaludapp.presentation.view.Activity

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.remote.FirebaseService
import com.miyo.doctorsaludapp.data.repository.LoginRepositoryImpl
import com.miyo.doctorsaludapp.databinding.ActivityRegisterBinding
import com.miyo.doctorsaludapp.domain.usecase.LoginUseCase
import com.miyo.doctorsaludapp.domain.usecase.RegisterUseCase
import com.miyo.doctorsaludapp.presentation.view.Fragment.SpecializationFragment
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModel
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModelFactory

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    // Inicializando los casos de uso y la fábrica del ViewModel
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
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val firstName = binding.firstNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val specialization = binding.specializationEditText.text.toString()
            val hospital = binding.hospitalEditText.text.toString()
            val license = binding.licenseEditText.text.toString()

            val bundle = Bundle().apply {
                putString("email", email)
                putString("password", password)
                putString("firstName", firstName)
                putString("lastName", lastName)
                putString("specialization", specialization)
                putString("hospital", hospital)
                putString("license", license)
            }

            val fragment = SpecializationFragment()
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.loginLink.setOnClickListener {
            finish()
        }

        authViewModel.user.observe(this, { user ->
            user?.let {
                // Manejar el registro exitoso
            }
        })

        authViewModel.error.observe(this, { error ->
            error?.let {
                // Manejar el error
            }
        })
    }
}