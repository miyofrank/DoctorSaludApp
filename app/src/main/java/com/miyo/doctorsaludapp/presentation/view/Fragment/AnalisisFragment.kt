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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.ai.GeminiAnalyzer
import com.miyo.doctorsaludapp.data.repository.FirebaseStorageRepository
import com.miyo.doctorsaludapp.data.repository.FirestoreEcgRepository
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.databinding.FragmentAnalisisBinding
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.ecg.MarkEcgAnalysisStartUseCase
import com.miyo.doctorsaludapp.domain.usecase.ecg.SaveEcgAnalysisUseCase
import com.miyo.doctorsaludapp.domain.usecase.ecg.UpsertEcgMetaUseCase
import com.miyo.doctorsaludapp.domain.usecase.patient.GetPatientsUseCase
import com.miyo.doctorsaludapp.domain.usecase.patient.SetPatientUseCase
import com.miyo.doctorsaludapp.presentation.view.Activity.PatientDetailActivity
import com.miyo.doctorsaludapp.presentation.view.Activity.RegisterPatientActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

class AnalisisFragment : Fragment() {

    private var _binding: FragmentAnalisisBinding? = null
    private val binding get() = _binding!!

    // Estado de interacción
    private var isUploading = false
    private var isAnalyzing = false
    private var isPicking = false

    // Firebase
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    // Repos y UseCases
    private val patientRepo by lazy { FirestorePatientRepository(firestore, "pacientes") }
    private val ecgRepo by lazy { FirestoreEcgRepository(firestore, "pacientes") }

    private val getPatientsUseCase by lazy { GetPatientsUseCase(patientRepo) }
    private val setPatientUseCase by lazy { SetPatientUseCase(patientRepo) }
    private val upsertEcgMetaUseCase by lazy { UpsertEcgMetaUseCase(ecgRepo) }
    private val saveEcgAnalysisUseCase by lazy { SaveEcgAnalysisUseCase(ecgRepo) }
    private val markEcgAnalysisStartUseCase by lazy { MarkEcgAnalysisStartUseCase(ecgRepo) }


    private val storageRepo by lazy { FirebaseStorageRepository(storage, requireContext().contentResolver) }

    // Pacientes
    private val pacientes = mutableListOf<Patient>()
    private var selected: Patient? = null

    // File picker
    private val pickEcg = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: run { isPicking = false; refreshButtons(); return@registerForActivityResult }
        val mime = requireContext().contentResolver.getType(uri) ?: ""
        if (mime.startsWith("image/")) {
            showPreviewFromUri(uri)
            binding.tvUploadHint.text = "Imagen seleccionada"
        } else {
            binding.ivEcgPreview.isVisible = false
            binding.tvUploadHint.text = "Archivo seleccionado"
        }
        uploadEcg(uri)
    }

    private var autoAnalisisEnabled = false

    // --------------------------------------------------------------------------------------------

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalisisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observePatients()
        setupUi()
    }

    // --------------------------------------------------------------------------------------------

    private fun setupUi() = with(binding) {
        acPaciente.setOnItemClickListener { _, _, position, _ ->
            val display = acPaciente.adapter.getItem(position) as String
            selected = pacientes.find { toDisplay(it) == display }
            showSelected()
            refreshButtons()
        }

        btnCrearPaciente.setOnClickListener {
            if (isUploading || isAnalyzing) return@setOnClickListener
            startActivity(Intent(requireContext(), RegisterPatientActivity::class.java))
        }
        btnVerDetalle.setOnClickListener {
            if (isUploading || isAnalyzing) return@setOnClickListener
            val id = selected?.id ?: return@setOnClickListener
            startActivity(Intent(requireContext(), PatientDetailActivity::class.java).putExtra("patient_id", id))
        }

        boxUpload.setOnClickListener {
            if (isUploading || isAnalyzing) return@setOnClickListener
            val hasPatient = selected?.id?.isNotBlank() == true
            if (!hasPatient) {
                Toast.makeText(requireContext(), "Selecciona o crea un paciente primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isPicking = true
            refreshButtons()
            pickEcg.launch(arrayOf("image/*", "application/pdf"))
        }

        btnAnalizar.text = getString(R.string.analizar_gemini)
        btnAnalizar.setOnClickListener {
            if (isUploading || isAnalyzing) return@setOnClickListener
            runGeminiAnalysis()
        }

        refreshButtons()
    }

    private fun refreshButtons() = with(binding) {
        val hasPatient = selected?.id?.isNotBlank() == true
        val hasEcg = selected?.ecgUrl?.isNotBlank() == true

        btnAnalizar.isEnabled = hasPatient && hasEcg && !isAnalyzing && !isUploading && !isPicking
        boxUpload.isEnabled = hasPatient && !isAnalyzing && !isUploading && !isPicking

        btnAnalizar.alpha = if (btnAnalizar.isEnabled) 1f else 0.5f
        boxUpload.alpha = if (boxUpload.isEnabled) 1f else 0.6f
    }

    private fun showLoading(loading: Boolean) = with(binding) { progressBar.isVisible = loading }

    private fun showPreviewFromUri(uri: Uri) = with(binding) {
        ivEcgPreview.isVisible = true
        ivEcgPreview.load(uri)
    }
    private fun showPreviewFromUrl(url: String) = with(binding) {
        ivEcgPreview.isVisible = true
        ivEcgPreview.load(url)
    }

    // --------------------------------------------------------------------------------------------

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

        if (!p?.ecgUrl.isNullOrBlank() && (p?.ecgMime?.startsWith("image/") == true)) {
            showPreviewFromUrl(p!!.ecgUrl!!)
            tvUploadHint.text = "ECG cargado"
        } else {
            binding.ivEcgPreview.isVisible = false
            tvUploadHint.text = "Toca para seleccionar archivo ECG"
        }
        refreshButtons()
    }


    // --------------------------------------------------------------------------------------------
    // Subida a Storage + creación/actualización del documento ECG
    // --------------------------------------------------------------------------------------------

    private fun uploadEcg(uri: Uri) {
        val p = selected ?: run {
            Toast.makeText(requireContext(), "Selecciona un paciente primero", Toast.LENGTH_SHORT).show()
            isPicking = false; refreshButtons()
            return
        }
        if (isUploading || isAnalyzing) return

        val mime = requireContext().contentResolver.getType(uri) ?: ""

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                isUploading = true
                showLoading(true)
                refreshButtons()
                binding.tvEcgStatus.text = "Subiendo ECG…"

                val path = "patients/${p.id}/ecg/ecg_${System.currentTimeMillis()}"
                val url = storageRepo.uploadSingle(uri, path)

                val updated = p.copy(
                    ecgUrl = url,
                    ecgId = path.substringAfterLast('/'),
                    ecgMime = mime
                )
                // 1) Actualiza al paciente (compat con tu app)
                setPatientUseCase(p.id!!, updated)
                selected = updated

                // 2) Crea/actualiza documento del ECG en subcolección
                upsertEcgMetaUseCase(
                    patientId = p.id!!,
                    ecgId = updated.ecgId!!,
                    url = url,
                    mime = mime,
                    storagePath = path
                )

                // Preview
                if (mime.startsWith("image/")) {
                    showPreviewFromUrl(url)
                    binding.tvUploadHint.text = "Imagen subida"
                } else {
                    binding.ivEcgPreview.isVisible = false
                    binding.tvUploadHint.text = "Archivo subido"
                }

                showSelected()
                Toast.makeText(requireContext(), "ECG cargado", Toast.LENGTH_SHORT).show()
                if (autoAnalisisEnabled) runGeminiAnalysis()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al subir ECG: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isUploading = false
                isPicking = false
                showLoading(false)
                refreshButtons()
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Análisis con Gemini + guardado de resultado en ecgs/{ecgId}
    // --------------------------------------------------------------------------------------------

    private fun runGeminiAnalysis() {
        val p = selected
        if (p?.id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Selecciona un paciente", Toast.LENGTH_SHORT).show()
            return
        }
        if (p?.ecgUrl.isNullOrBlank() || p?.ecgId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Carga un ECG para continuar", Toast.LENGTH_SHORT).show()
            return
        }

        // deshabilitar UI mientras corre
        binding.btnAnalizar.isEnabled = false
        binding.boxUpload.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val startNs = System.nanoTime()
            try {
                // 1) MARCAR INICIO en servidor (timestamps)
                markEcgAnalysisStartUseCase(p.id!!, p.ecgId!!)

                // 2) Descarga bytes y llama a Gemini
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(p.ecgUrl!!)
                val bytes = ref.getBytes(10L * 1024L * 1024L).await()
                val mime = p.ecgMime ?: "image/jpeg"

                val ai = GeminiAnalyzer.analyze(requireContext(), bytes, mime)

                val analysis = EcgAnalysis(
                    ritmo = ai.ritmo,
                    fc_bpm = ai.fc_bpm,
                    pr_ms = ai.pr_ms,
                    qrs_ms = ai.qrs_ms,
                    qt_ms = ai.qt_ms,
                    qtc_ms = ai.qtc_ms,
                    precisionIA = ai.precisionIA,
                    nivelRiesgo = ai.nivelRiesgo,
                    interpretacion = ai.interpretacion,
                    recomendacion = ai.recomendacion,
                    updatedAt = java.util.Date(),
                    source = "gemini"
                )

                // 3) Duración exacta en ms medida en cliente
                val durationMs = ((System.nanoTime() - startNs) / 1_000_000.0).toLong()

                // 4) GUARDAR resultado + FIN + durationMs
                saveEcgAnalysisUseCase(p.id!!, p.ecgId!!, analysis, durationMs)

                // 5) Actualizar UI
                val iaSec = durationMs / 1000.0
                binding.tvInterpretacion.text = analysis.interpretacion ?: "—"
                binding.tvPrecision.text = "Precisión de IA: ${analysis.precisionIA?.let { "%.1f".format(it) } ?: "—"}%"
                binding.tvRiesgo.text = "Nivel de riesgo: ${analysis.nivelRiesgo ?: "—"}"
                binding.tvParametros.text = "FC: ${analysis.fc_bpm ?: "—"} bpm   Ritmo: ${analysis.ritmo ?: "—"}   PR: ${analysis.pr_ms ?: "—"} ms   QRS: ${analysis.qrs_ms ?: "—"} ms   QT: ${analysis.qt_ms ?: "—"} ms   QTc: ${analysis.qtc_ms ?: "—"} ms"
                binding.tvTiempoIA.text = "Tiempo con IA: ${"%.1f".format(iaSec)}s"
                binding.tvTiempoManual.text = "Tiempo manual estimado: 15.5 min"
                binding.tvAhorro.text = "Tiempo ahorrado: ${"%.1f".format(15.5 - iaSec/60.0)} min"

                Toast.makeText(requireContext(), "Análisis guardado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error en análisis: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnAnalizar.isEnabled = true
                binding.boxUpload.isEnabled = true
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
