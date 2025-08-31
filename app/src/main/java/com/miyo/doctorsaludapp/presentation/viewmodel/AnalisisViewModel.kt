package com.miyo.doctorsaludapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.domain.model.Paciente
import com.miyo.doctorsaludapp.domain.usecase.GenerarDiagnosticosUseCase
import com.miyo.doctorsaludapp.domain.usecase.GenerarRecomendacionesUseCase
import kotlinx.coroutines.launch

class AnalisisViewModel(application: Application) : AndroidViewModel(application) {

    private val _diagnostico = MutableLiveData<String>()
    val diagnostico: LiveData<String> get() = _diagnostico

    private val _recomendaciones = MutableLiveData<List<String>>()
    val recomendaciones: LiveData<List<String>> get() = _recomendaciones

    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    private val generarDiagnosticosUseCase = GenerarDiagnosticosUseCase(application)
    private val generarRecomendacionesUseCase = GenerarRecomendacionesUseCase()

    init {
        // Cargar pacientes desde Firestore
        FirebaseFirestore.getInstance().collection("paciente")
            .get()
            .addOnSuccessListener { result ->
                val pacientesList = result.map { document ->
                    document.toObject(Paciente::class.java).apply { id = document.id }
                }
                _pacientes.value = pacientesList
            }
    }

    fun generarDiagnostico(
        paciente: Paciente,
        dolorPecho: Int, presionArterial: Float, colesterol: Float, azucarEnSangre: Int,
        restEcg: Int, frecuenciaCardiaca: Float, anginaEjercicio: Int, oldpeak: Float,
        pendiente: Int, vasosColoreados: Int, talasemia: Int
    ) {
        viewModelScope.launch {
            val diagnostico = generarDiagnosticosUseCase.ejecutar(
                paciente, dolorPecho, presionArterial, colesterol, azucarEnSangre,
                restEcg, frecuenciaCardiaca, anginaEjercicio, oldpeak, pendiente,
                vasosColoreados, talasemia
            )
            _diagnostico.value = diagnostico

            // Crear el informe con todos los datos del modelo
            val informe = mapOf(
                "pacienteId" to paciente.id,
                "diagnostico" to diagnostico,
                "edad" to paciente.edad,
                "genero" to paciente.genero,
                "dolorPecho" to dolorPecho,
                "presionArterial" to presionArterial,
                "colesterol" to colesterol,
                "azucarEnSangre" to azucarEnSangre,
                "restEcg" to restEcg,
                "frecuenciaCardiaca" to frecuenciaCardiaca,
                "anginaEjercicio" to anginaEjercicio,
                "oldpeak" to oldpeak,
                "pendiente" to pendiente,
                "vasosColoreados" to vasosColoreados,
                "talasemia" to talasemia
            )
            // Guardar el resultado en Firebase Realtime Database
            FirebaseDatabase.getInstance().reference.child("Informe").push().setValue(informe)

            // Generar recomendaciones basadas en los parámetros de salud
            val recomendaciones = generarRecomendacionesUseCase.generarRecomendaciones(
                presionArterial, colesterol, azucarEnSangre, frecuenciaCardiaca, oldpeak
            )
            _recomendaciones.value = recomendaciones

            // Guardar las recomendaciones en la colección "recomendaciones" de Firestore
            val recomendacion = hashMapOf(
                "nombrePaciente" to paciente.nombre,
                "recomendaciones" to recomendaciones
            )
            FirebaseFirestore.getInstance().collection("recomendaciones").add(recomendacion)
                .addOnSuccessListener {
                    // Las recomendaciones se guardaron exitosamente
                }
                .addOnFailureListener { e ->
                    // Manejar el error si las recomendaciones no se guardaron
                    e.printStackTrace()
                }
        }
    }
}


