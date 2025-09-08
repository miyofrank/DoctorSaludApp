package com.miyo.doctorsaludapp.presentation.view.Activity

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirestoreUserRepository
import com.miyo.doctorsaludapp.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userRepo by lazy { FirestoreUserRepository(FirebaseFirestore.getInstance()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usa el nombre real de tu layout aquí (ajústalo si lo tuyo no es activity_register)
        setContentView(R.layout.activity_register)

        val emailEt: EditText        = findViewById(R.id.emailEditText)
        val passEt: EditText         = findViewById(R.id.passwordEditText)
        val nombresEt: EditText      = findViewById(R.id.firstNameEditText)
        val apellidosEt: EditText    = findViewById(R.id.lastNameEditText)
        val especEt: EditText        = findViewById(R.id.specializationEditText)
        val hospitalEt: EditText     = findViewById(R.id.hospitalEditText)
        val colegEt: EditText        = findViewById(R.id.licenseEditText)
        val btnRegister: Button      = findViewById(R.id.registerButton)

        btnRegister.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString()
            val nombres = nombresEt.text.toString().trim()
            val apellidos = apellidosEt.text.toString().trim()
            val especialidad = especEt.text.toString().trim()
            val hospital = hospitalEt.text.toString().trim()
            val coleg = colegEt.text.toString().trim()

            if (email.isEmpty() || pass.length < 6 || nombres.isEmpty() || apellidos.isEmpty()) {
                Toast.makeText(this, "Completa email/contraseña (≥6) y nombre/apellidos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // 1) Crear usuario
                    val res = auth.createUserWithEmailAndPassword(email, pass).await()
                    val uid = res.user?.uid ?: throw IllegalStateException("Sin UID")

                    // 2) Guardar perfil en `usuarios/{uid}`
                    val profile = UserProfile(
                        id = uid,
                        nombres = nombres,
                        apellidos = apellidos,
                        email = email,
                        especialidad = especialidad,
                        hospital = hospital,
                        colegiatura = coleg,
                        telefono = null,
                        autoAnalisis = false
                    )
                    withContext(Dispatchers.IO) {
                        userRepo.set(uid, profile)
                    }

                    Toast.makeText(this@RegisterActivity, "Cuenta creada", Toast.LENGTH_SHORT).show()
                    finish() // vuelve a login o navega a Home si corresponde

                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
