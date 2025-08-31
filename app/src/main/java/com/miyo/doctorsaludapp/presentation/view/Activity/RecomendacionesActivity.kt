package com.miyo.doctorsaludapp.presentation.view.Activity

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.databinding.ActivityRecomendacionesBinding
import com.miyo.doctorsaludapp.presentation.viewmodel.AnalisisViewModel
import com.miyo.doctorsaludapp.presentation.viewmodel.RecomendacionesViewModel

class RecomendacionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecomendacionesBinding
    private lateinit var viewModel: RecomendacionesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityRecomendacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(RecomendacionesViewModel::class.java)

        val nombrePaciente = intent.getStringExtra("nombrePaciente") ?: ""
        if (nombrePaciente.isNotEmpty()) {
            viewModel.cargarRecomendaciones(nombrePaciente)
        }

        setupObservers()
    }

    private fun setupObservers() {

        viewModel.recomendaciones.observe(this, Observer { recomendaciones ->
            binding.tvRecomendaciones.text = recomendaciones.joinToString("\n")
        })

        viewModel.apto.observe(this, Observer { apto ->
            val mensajeApto = if (apto) {
                "El paciente es apto."
            } else {
                "El paciente no es apto."
            }
            Toast.makeText(this, mensajeApto, Toast.LENGTH_LONG).show()
        })
    }
}

