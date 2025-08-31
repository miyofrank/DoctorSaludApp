package com.miyo.doctorsaludapp.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.miyo.doctorsaludapp.domain.model.Paciente
import com.miyo.doctorsaludapp.domain.usecase.PacienteUseCase

class RegisterViewModel : ViewModel() {
    private val pacienteUseCase = PacienteUseCase()

    fun registerPatient(
        name: String, surname: String, email: String, address: String,
        age: Int, date: String, gender: String, phone: Int, fotoUri: Uri?
    ) {
        if (fotoUri != null) {
            pacienteUseCase.cargarFotoPaciente(fotoUri, { fotoUrl ->
                val paciente = Paciente(
                    nombre = name,
                    apellidos = surname,
                    correo = email,
                    direccion = address,
                    edad = age,
                    fecha = date,
                    genero = gender,
                    telefono = phone,
                    fotoUrl = fotoUrl
                )
                registrarPacienteEnFirestore(paciente)
            }, {
                // handle failure
            })
        } else {
            val paciente = Paciente(
                nombre = name,
                apellidos = surname,
                correo = email,
                direccion = address,
                edad = age,
                fecha = date,
                genero = gender,
                telefono = phone
            )
            registrarPacienteEnFirestore(paciente)
        }
    }

    private fun registrarPacienteEnFirestore(paciente: Paciente) {
        pacienteUseCase.registrarPaciente(paciente, {
            // handle success
        }, {
            // handle failure
        })
    }
}
