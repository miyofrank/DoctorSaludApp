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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.ai.GeminiAnalyzer // <-- NUEVO}
import kotlinx.coroutines.tasks.await
import com.miyo.doctorsaludapp.data.repository.FirebaseStorageRepository
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
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

        // Cargar ECG (por ahora aceptamos imágenes y pdf; OJO: Gemini solo procesa imagen)
        boxUpload.setOnClickListener {
            pickEcg.launch(arrayOf("image/*", "application/pdf"))
        }

        // Analizar (Gemini)
        btnAnalizar.text = getString(R.string.analizar_gemini) // cambia el texto si tienes el string
        btnAnalizar.setOnClickListener { runGeminiAnalysis() }
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
            Toast.makeText(requireContext(), "Selecciona un paciente primero", Toast.LENGTH_SHORT).show()
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
                if (autoAnalisisEnabled) runGeminiAnalysis()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al subir ECG: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Analiza el ECG con Gemini (JSON mode) y rellena UI + guarda riesgo en el paciente.
     * IMPORTANTE: ahora devuelve Unit (bloque), no expresión.
     */
    private fun runGeminiAnalysis() {
        val startTs = System.nanoTime()

        with(binding) {
            val p = selected
            if (p?.id.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Selecciona un paciente", Toast.LENGTH_SHORT).show()
                return
            }
            if (p?.ecgUrl.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Carga un ECG para continuar", Toast.LENGTH_SHORT).show()
                return
            }
            if (p.ecgMime?.contains("pdf", ignoreCase = true) == true) {
                Toast.makeText(requireContext(), "Por ahora Gemini requiere imagen (JPG/PNG), no PDF.", Toast.LENGTH_LONG).show()
                return
            }

            // Limpia UI mientras analiza
            tvInterpretacion.text = "Analizando…"
            tvPrecision.text = "Precisión de IA: —"
            tvRiesgo.text = "Nivel de riesgo: —"
            tvParametros.text = "FC: —   Ritmo: —   PR: —   QRS: —   QT: —   QTc: —"
            tvTiempoIA.text = "Tiempo con IA: —"
            tvTiempoManual.text = "Tiempo manual estimado: 15-20 min"
            tvAhorro.text = "Tiempo ahorrado: —"

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // 1) Descargar bytes desde el downloadUrl guardado en el paciente
                    val ref = storage.getReferenceFromUrl(p.ecgUrl!!)
                    val bytes = ref.getBytes(10L * 1024L * 1024L).await() // hasta 10MB
                    val mime = p.ecgMime ?: sniffMime(bytes)

                    // 2) Llamar a Gemini (JSON mode)
                    val result = GeminiAnalyzer.analyze(requireContext(), bytes, mime)

                    // 3) Rellenar UI con el resultado
                    tvInterpretacion.text = result.interpretacion.ifBlank { "—" }
                    tvPrecision.text = "Precisión de IA: ${result.precisionIA?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—"}"
                    tvRiesgo.text = "Nivel de riesgo: ${result.nivelRiesgo}"
                    tvParametros.text = buildString {
                        append("FC: ${result.fc_bpm ?: "-"} bpm   ")
                        append("Ritmo: ${result.ritmo}   ")
                        append("PR: ${result.pr_ms ?: "-"} ms   ")
                        append("QRS: ${result.qrs_ms ?: "-"} ms   ")
                        append("QT: ${result.qt_ms ?: "-"} ms   ")
                        append("QTc: ${result.qtc_ms ?: "-"} ms")
                    }

                    val iaTimeMs = (System.nanoTime() - startTs) / 1_000_000.0
                    tvTiempoIA.text = "Tiempo con IA: ${String.format(Locale.getDefault(), "%.1f", iaTimeMs/1000.0)}s"
                    tvAhorro.text = "Tiempo ahorrado: ~18 min"

                    // 4) Guardar riesgo/porcentaje simplificado en el paciente
                    val pct = when (result.nivelRiesgo.lowercase(Locale.getDefault())) {
                        "alto" -> 85
                        "moderado" -> 55
                        else -> 15
                    }
                    val updated = p!!.copy(
                        riesgo = result.nivelRiesgo.lowercase(Locale.getDefault()),
                        riesgoPct = pct
                    )
                    setPatientUseCase(p.id!!, updated)
                    selected = updated
                    showSelected()

                    Toast.makeText(requireContext(), "Análisis guardado", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    tvInterpretacion.text = "—"
                    Toast.makeText(requireContext(), "Error al analizar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Sniffer simple si no tenemos mime guardado */
    private fun sniffMime(bytes: ByteArray): String {
        return when {
            bytes.size >= 8 &&
                    bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
            bytes.size >= 3 &&
                    bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
                    bytes[2] == 0xFF.toByte() -> "image/jpeg"
            else -> "image/png"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
