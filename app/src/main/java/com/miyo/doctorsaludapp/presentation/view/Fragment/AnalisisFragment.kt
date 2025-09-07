package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirebaseStorageRepository
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.data.repository.FirestoreUserRepository
import com.miyo.doctorsaludapp.databinding.FragmentAnalisisBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.patient.GetPatientsUseCase
import com.miyo.doctorsaludapp.domain.usecase.patient.SetPatientUseCase
import com.miyo.doctorsaludapp.presentation.view.Activity.PatientDetailActivity
import com.miyo.doctorsaludapp.presentation.view.Activity.RegisterPatientActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class AnalisisFragment : Fragment() {

    private var _binding: FragmentAnalisisBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private val repo by lazy { FirestorePatientRepository(firestore, "pacientes") }
    private val getPatientsUseCase by lazy { GetPatientsUseCase(repo) }
    private val setPatientUseCase by lazy { SetPatientUseCase(repo) }
    private val storageRepo by lazy { FirebaseStorageRepository(storage, requireContext().contentResolver) }

    private val pacientes = mutableListOf<Patient>()
    private var selected: Patient? = null

    private val pickEcg = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        uploadEcg(uri)
    }
    private var autoAnalisisEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalisisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observePatients()
        setupUi()
    }

    private fun setupUi() = with(binding) {
        // Selección de paciente
        acPaciente.setOnItemClickListener { _, _, position, _ ->
            val display = acPaciente.adapter.getItem(position) as String
            selected = pacientes.find { toDisplay(it) == display }
            showSelected()
        }

        btnCrearPaciente.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterPatientActivity::class.java))
        }
        btnVerDetalle.setOnClickListener {
            val id = selected?.id ?: return@setOnClickListener
            startActivity(
                Intent(requireContext(), PatientDetailActivity::class.java)
                    .putExtra("patient_id", id)
            )
        }

        // Carga ECG
        boxUpload.setOnClickListener {
            pickEcg.launch(arrayOf("image/*", "application/pdf"))
        }

        // Analizar (mock)
        btnAnalizar.setOnClickListener { runMockAnalysis() }
    }

    private fun observePatients() {
        viewLifecycleOwner.lifecycleScope.launch {
            getPatientsUseCase().collectLatest { list ->
                pacientes.clear()
                pacientes.addAll(list)

                val displays = pacientes.map { toDisplay(it) }
                binding.acPaciente.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displays)
                )
            }
        }
    }

    private fun toDisplay(p: Patient): String {
        val nombre = when {
            !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
            else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
        }
        val dni = p.dni.orEmpty()
        return if (dni.isNotBlank()) "$nombre – $dni" else nombre
    }

    private fun showSelected() = with(binding) {
        val p = selected
        tvPacienteSeleccionado.text = "Paciente: " + (p?.let { toDisplay(it) } ?: "—")
        tvEcgStatus.text = if (p?.ecgUrl.isNullOrBlank()) "Sin ECG cargado" else "ECG cargado"
    }

    private fun uploadEcg(uri: Uri) {
        val p = selected ?: run {
            Toast.makeText(requireContext(), "Selecciona un paciente primero", Toast.LENGTH_SHORT)
                .show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val path = "patients/${p.id}/ecg/ecg_${System.currentTimeMillis()}"
                val url = storageRepo.uploadSingle(uri, path)
                val updated = p.copy(
                    ecgUrl = url,
                    ecgId = path.substringAfterLast('/'),
                    ecgMime = requireContext().contentResolver.getType(uri)
                )
                setPatientUseCase(p.id!!, updated)
                selected = updated
                showSelected()
                Toast.makeText(requireContext(), "ECG cargado", Toast.LENGTH_SHORT).show()
                if (autoAnalisisEnabled) runMockAnalysis()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al subir ECG: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * IMPORTANTE: ahora devuelve Unit (bloque), no expresión.
     * Así evitamos el error de "expected Job, actual Unit" en los returns tempranos.
     */
    private fun runMockAnalysis() {
        with(binding) {
            val p = selected
            if (p?.id.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Selecciona un paciente", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            if (p?.ecgUrl.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Carga un ECG para continuar", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            // MOCK: resultados bonitos
            val riesgo = listOf("Bajo", "Moderado", "Alto")[Random.nextInt(3)]
            val pct = listOf(92, 94, 95, 96).random()
            val ritmo = listOf("Sinusal", "Irregular").random()
            val fc = listOf(76, 78, 80).random()
            val pr = listOf(160, 170).random()
            val qrs = listOf(90, 100).random()
            val qt = listOf(390, 400, 410).random()
            val qtc = listOf(420, 430).random()

            tvInterpretacion.text = "ECG dentro de parámetros normales"
            tvPrecision.text = "Precisión de IA: ${pct}%"
            tvRiesgo.text = "Nivel de riesgo: $riesgo"
            tvParametros.text =
                "FC: ${fc} bpm   Ritmo: $ritmo   PR: ${pr} ms   QRS: ${qrs} ms   QT: ${qt} ms   QTc: ${qtc} ms"

            tvTiempoIA.text = "Tiempo con IA: 2.3s"
            tvTiempoManual.text = "Tiempo manual estimado: 15-20 min"
            tvAhorro.text = "Tiempo ahorrado: ~18 min"

            // Guardar en el paciente (solo riesgo/pct mock por ahora)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val updated = p!!.copy(
                        riesgo = riesgo.lowercase(Locale.getDefault()),
                        riesgoPct = pct
                    )
                    setPatientUseCase(p.id!!, updated)
                    selected = updated
                    Toast.makeText(
                        requireContext(),
                        "Resultados guardados (mock)",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (autoAnalisisEnabled) runMockAnalysis()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo guardar resultados: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}





