package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.miyo.doctorsaludapp.domain.model.Paciente
import com.miyo.doctorsaludapp.domain.usecase.PacienteUseCase

class PacientesViewModel : ViewModel() {
    private val pacienteUseCase = PacienteUseCase()
    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    fun getPacientes() {
        pacienteUseCase.listarPacientes({ pacientesList ->
            _pacientes.value = pacientesList
        }, {
            // handle failure
        })
    }

    fun eliminarPaciente(pacienteId: String) {
        pacienteUseCase.eliminarPaciente(pacienteId, {
            // handle success
            getPacientes() // refresh list after deletion
        }, {
            // handle failure
        })
    }

}