package com.miyo.doctorsaludapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.domain.model.Informe

class RecomendacionesViewModel(application: Application) : AndroidViewModel(application) {

    private val _recomendaciones = MutableLiveData<List<String>>()
    val recomendaciones: LiveData<List<String>> get() = _recomendaciones

    private val _apto = MutableLiveData<Boolean>()
    val apto: LiveData<Boolean> get() = _apto

    fun cargarRecomendaciones(nombrePaciente: String) {
        // Cargar recomendaciones desde Firestore
        FirebaseFirestore.getInstance().collection("recomendaciones")
            .whereEqualTo("nombrePaciente", nombrePaciente)
            .get()
            .addOnSuccessListener { result ->
                val recomendacionesList = result.documents.flatMap { document ->
                    document.get("recomendaciones") as List<String>
                }
                _recomendaciones.value = recomendacionesList
            }

        // Cargar estado de aptitud desde Firebase Realtime Database
        FirebaseDatabase.getInstance().reference.child("Informe")
            .orderByChild("nombrePaciente")
            .equalTo(nombrePaciente)
            .get()
            .addOnSuccessListener { snapshot ->
                val informe = snapshot.children.firstOrNull()?.getValue(Informe::class.java)
                _apto.value = informe?.apto
            }
    }
}

