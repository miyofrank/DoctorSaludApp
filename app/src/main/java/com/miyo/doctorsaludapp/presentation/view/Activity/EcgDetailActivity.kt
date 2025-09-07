package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.data.repository.FirebaseStorageRepository
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.databinding.ActivityEcgDetailBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.patient.GetPatientByIdUseCase
import com.miyo.doctorsaludapp.domain.usecase.patient.SetPatientUseCase
import kotlinx.coroutines.launch
import java.util.Locale

class EcgDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEcgDetailBinding

    private val repo by lazy { FirestorePatientRepository(FirebaseFirestore.getInstance(), "pacientes") }
    private val getById by lazy { GetPatientByIdUseCase(repo) }
    private val setById by lazy { SetPatientUseCase(repo) }

    private val storage by lazy { FirebaseStorage.getInstance() }
    private val storageRepo by lazy { FirebaseStorageRepository(storage, contentResolver) }

    private var patientId: String? = null
    private var patient: Patient? = null

    private val pickEcg = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        uploadNewEcg(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEcgDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        patientId = intent.getStringExtra("patient_id")
        if (patientId.isNullOrEmpty()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        setupToolbar()
        setupActions()
        loadPatient()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "ECG"
    }

    private fun setupActions() = with(binding) {
        btnSubirEcg.setOnClickListener {
            pickEcg.launch(arrayOf("image/*", "application/pdf"))
        }
        btnAbrirEcg.setOnClickListener { openEcg() }
        btnVerDetalle.setOnClickListener {
            startActivity(Intent(this@EcgDetailActivity, PatientDetailActivity::class.java).putExtra("patient_id", patientId))
        }
        btnNuevoAnalisis.setOnClickListener {
            Toast.makeText(this@EcgDetailActivity, "Análisis IA pendiente de integrar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPatient() {
        lifecycleScope.launch {
            val p = getById(patientId!!)
            if (p == null) {
                Toast.makeText(this@EcgDetailActivity, "Paciente no encontrado", Toast.LENGTH_SHORT).show()
                finish(); return@launch
            }
            patient = p
            bindPatient(p)
        }
    }

    private fun bindPatient(p: Patient) = with(binding) {
        val nombre = when {
            !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
            else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
        }
        tvNombre.text = nombre
        tvDniEdad.text = "DNI: ${p.dni.orEmpty()} • ${p.edad?.toString() ?: ""} años"

        if (p.ecgUrl.isNullOrBlank()) {
            tvEcgStatus.text = "Sin ECG cargado"
            btnAbrirEcg.isEnabled = false
        } else {
            tvEcgStatus.text = "ECG cargado"
            btnAbrirEcg.isEnabled = true
        }

        // Mock de resultados por ahora
        tvInterpretacion.text = "Interpretación: ${p.riesgo?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "—"}"
        tvPrecision.text = "Precisión de IA: ${p.riesgoPct?.let { "$it%" } ?: "—"}"
        tvRiesgo.text = "Nivel de riesgo: ${p.riesgo ?: "—"}"
        tvParametros.text = "FC: —  Ritmo: —  PR: —  QRS: —  QT: —  QTc: —"
    }

    private fun openEcg() {
        val url = patient?.ecgUrl ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No hay app para abrir el ECG", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadNewEcg(uri: Uri) {
        val id = patientId ?: return
        lifecycleScope.launch {
            try {
                val path = "patients/$id/ecg/ecg_${System.currentTimeMillis()}"
                val url = storageRepo.uploadSingle(uri, path)
                val mime = contentResolver.getType(uri)
                val updated = (patient ?: Patient()).copy(
                    id = id,
                    ecgUrl = url,
                    ecgId = path.substringAfterLast('/'),
                    ecgMime = mime
                )
                setById(id, updated)
                patient = updated
                bindPatient(updated)
                Toast.makeText(this@EcgDetailActivity, "ECG actualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EcgDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
