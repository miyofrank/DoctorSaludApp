package com.miyo.doctorsaludapp.presentation.view.Fragment
import android.R.layout
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.miyo.doctorsaludapp.R
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.miyo.doctorsaludapp.databinding.FragmentAnalisisBinding
import com.miyo.doctorsaludapp.domain.model.Paciente
import com.miyo.doctorsaludapp.domain.usecase.PacienteUseCase
import com.miyo.doctorsaludapp.presentation.view.Activity.RecomendacionesActivity
import com.miyo.doctorsaludapp.presentation.viewmodel.AnalisisViewModel


class AnalisisFragment : Fragment() {

    private lateinit var binding: FragmentAnalisisBinding
    private lateinit var viewModel: AnalisisViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAnalisisBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(AnalisisViewModel::class.java)

        setupUI()
        setupObservers()

        return binding.root
    }

    private fun setupUI() {
        // Setup Spinners
        val dolorPechoAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.dolor_pecho_array,
            android.R.layout.simple_spinner_item
        )
        dolorPechoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDolorPecho.adapter = dolorPechoAdapter

        val azucarEnSangreAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.azucar_en_sangre_array,
            android.R.layout.simple_spinner_item
        )
        azucarEnSangreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAzucarEnSangre.adapter = azucarEnSangreAdapter

        val restEcgAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.rest_ecg_array,
            android.R.layout.simple_spinner_item
        )
        restEcgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRestEcg.adapter = restEcgAdapter

        val anginaEjercicioAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.angina_ejercicio_array,
            android.R.layout.simple_spinner_item
        )
        anginaEjercicioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnginaEjercicio.adapter = anginaEjercicioAdapter

        val pendienteAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.pendiente_array,
            android.R.layout.simple_spinner_item
        )
        pendienteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPendiente.adapter = pendienteAdapter

        val vasosColoreadosAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.vasos_coloreados_array,
            android.R.layout.simple_spinner_item
        )
        vasosColoreadosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVasosColoreados.adapter = vasosColoreadosAdapter

        val talasemiaAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.talasemia_array,
            android.R.layout.simple_spinner_item
        )
        talasemiaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTalasemia.adapter = talasemiaAdapter

        binding.btnBuscarPaciente.setOnClickListener {
            val nombrePaciente = binding.etNombrePaciente.text.toString()
            buscarPaciente(nombrePaciente)
        }

        binding.btnGenerarDiagnostico.setOnClickListener {
            if (validarDatos()) {
                generarDiagnostico()
            }
        }

        binding.btnRecomendaciones.setOnClickListener {
            val nombrePaciente = binding.etNombrePaciente.text.toString()
            val intent = Intent(requireContext(), RecomendacionesActivity::class.java)
            intent.putExtra("nombrePaciente", nombrePaciente)
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        viewModel.diagnostico.observe(viewLifecycleOwner, Observer { diagnostico ->
            Toast.makeText(requireContext(), diagnostico, Toast.LENGTH_LONG).show()
        })
    }

    private val pacienteUseCase = PacienteUseCase()

    private fun buscarPaciente(nombre: String) {
        pacienteUseCase.buscarPacientePorNombre(nombre, { paciente ->
            paciente?.let {
                binding.etGenero.setText(it.genero)
                binding.etEdad.setText(it.edad.toString())
            } ?: run {
                Toast.makeText(requireContext(), "Paciente no encontrado", Toast.LENGTH_SHORT).show()
            }
        }, {
            Toast.makeText(requireContext(), "Error al buscar paciente", Toast.LENGTH_SHORT).show()
        })
    }

    private fun generarDiagnostico() {
        val presionArterial = binding.etPresionArterial.text.toString().toFloat()
        val colesterol = binding.etColesterol.text.toString().toFloat()
        val azucarEnSangre = binding.spinnerAzucarEnSangre.selectedItemPosition
        val restEcg = binding.spinnerRestEcg.selectedItemPosition
        val frecuenciaCardiaca = binding.etFrecuenciaCardiaca.text.toString().toFloat()
        val anginaEjercicio = binding.spinnerAnginaEjercicio.selectedItemPosition
        val oldpeak = binding.etOldpeak.text.toString().toFloat()
        val pendiente = binding.spinnerPendiente.selectedItemPosition
        val vasosColoreados = binding.spinnerVasosColoreados.selectedItemPosition
        val talasemia = binding.spinnerTalasemia.selectedItemPosition
        val dolorPecho = binding.spinnerDolorPecho.selectedItemPosition

        val nombrePaciente = binding.etNombrePaciente.text.toString()
        val genero = binding.etGenero.text.toString()
        val edad = binding.etEdad.text.toString().toInt()

        val paciente = Paciente(nombre = nombrePaciente, genero = genero, edad = edad)

        viewModel.generarDiagnostico(
            paciente,
            dolorPecho,
            presionArterial,
            colesterol,
            azucarEnSangre,
            restEcg,
            frecuenciaCardiaca,
            anginaEjercicio,
            oldpeak,
            pendiente,
            vasosColoreados,
            talasemia
        )
    }

    private fun validarDatos(): Boolean {
        val nombrePaciente = binding.etNombrePaciente.text.toString()
        val presionArterial = binding.etPresionArterial.text.toString()
        val colesterol = binding.etColesterol.text.toString()
        val frecuenciaCardiaca = binding.etFrecuenciaCardiaca.text.toString()
        val oldpeak = binding.etOldpeak.text.toString()
        val genero = binding.etGenero.text.toString()
        val edad = binding.etEdad.text.toString()

        return when {
            nombrePaciente.isEmpty() -> {
                binding.etNombrePaciente.error = "Este campo es obligatorio"
                false
            }
            presionArterial.isEmpty() -> {
                binding.etPresionArterial.error = "Este campo es obligatorio"
                false
            }
            colesterol.isEmpty() -> {
                binding.etColesterol.error = "Este campo es obligatorio"
                false
            }
            frecuenciaCardiaca.isEmpty() -> {
                binding.etFrecuenciaCardiaca.error = "Este campo es obligatorio"
                false
            }
            oldpeak.isEmpty() -> {
                binding.etOldpeak.error = "Este campo es obligatorio"
                false
            }
            genero.isEmpty() -> {
                binding.etGenero.error = "Este campo es obligatorio"
                false
            }
            edad.isEmpty() -> {
                binding.etEdad.error = "Este campo es obligatorio"
                false
            }
            else -> true
        }
    }
}




