package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.databinding.ActivityEcgDetailBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.patient.GetPatientByIdUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale

class EcgDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEcgDetailBinding

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val repo by lazy { FirestorePatientRepository(db, "pacientes") }
    private val getById by lazy { GetPatientByIdUseCase(repo) }

    private var patientId: String? = null
    private var patient: Patient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEcgDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        patientId = intent.getStringExtra("patient_id")
        if (patientId.isNullOrEmpty()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "ECG"

        binding.btnVerDetalle.setOnClickListener {
            startActivity(
                Intent(this, PatientDetailActivity::class.java)
                    .putExtra("patient_id", patientId!!)
            )
        }


        loadScreen()
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        progress.isVisible = loading
        content.isVisible = !loading
    }

    private fun loadScreen() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                // 1) Paciente
                val p = getById(patientId!!) ?: error("Paciente no encontrado")
                patient = p
                bindPatientHeader(p)

                // 2) Último ECG (con analysis)
                val lastEcg = db.collection("pacientes").document(patientId!!)
                    .collection("ecgs")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(1).get().await().documents.firstOrNull()
                    ?: db.collection("pacientes").document(patientId!!)
                        .collection("ecgs")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1).get().await().documents.firstOrNull()

                val ecgUrl = lastEcg?.getString("ecgUrl") ?: p.ecgUrl
                val ecgMime = lastEcg?.getString("ecgMime") ?: p.ecgMime ?: "image/jpeg"
                showEcgPreview(ecgUrl, ecgMime)

                val analysis = lastEcg?.get("analysis") as? Map<*, *>
                bindAnalysis(analysis)
            } catch (e: Exception) {
                Toast.makeText(this@EcgDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                bindAnalysis(null) // deja la UI en estado neutro
            } finally {
                setLoading(false)
            }
        }
    }

    private fun bindPatientHeader(p: Patient) = with(binding) {
        val nombre = when {
            !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
            else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
        }.ifBlank { "Paciente" }
        tvNombre.text = nombre
        tvDniEdad.text = "DNI: ${p.dni.orEmpty()} • ${p.edad?.toString() ?: "—"} años"
    }

    private fun showEcgPreview(url: String?, mime: String) = with(binding) {
        if (!url.isNullOrBlank() && mime.startsWith("image/")) {
            ivEcg.load(url) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_error)
                transformations(RoundedCornersTransformation(16f))
            }
            tvEcgStatus.text = "ECG cargado"
        } else {
            ivEcg.load(R.drawable.bg_image_placeholder)
            tvEcgStatus.text = if (url.isNullOrBlank()) "Sin ECG cargado" else "Archivo ECG (abrir para ver)"
        }
    }

    private fun bindAnalysis(a: Map<*, *>?) = with(binding) {
        // Reset visual
        tvInterpretacion.text = "Interpretación: —"
        tvRecomendacion.text  = "Recomendación: —"
        tvPrecision.text      = "Precisión de IA: —"
        tvRiesgo.text         = "Nivel de riesgo: —"
        tvParametros.text     = "FC: —  Ritmo: —  PR: —  QRS: —  QT: —  QTc: —"

        if (a == null) return@with

        val df0 = DecimalFormat("#0")
        val df1 = DecimalFormat("#0.0")

        val ritmo = a["ritmo"] as? String
        val fc    = (a["fc_bpm"] as? Number)?.toInt()
        val pr    = (a["pr_ms"] as? Number)?.toDouble()
        val qrs   = (a["qrs_ms"] as? Number)?.toDouble()
        val qt    = (a["qt_ms"] as? Number)?.toDouble()
        val qtc   = (a["qtc_ms"] as? Number)?.toDouble()
        val prec  = (a["precisionIA"] as? Number)?.toDouble()
        val riesgo = (a["nivelRiesgo"] as? String)
        val interpretacion = (a["interpretacion"] as? String)?.trim().orEmpty()
        val recomendacion  = (a["recomendacion"] as? String)?.trim().orEmpty()
        val analyzedAt: Date? = (a["analyzedAt"] as? Timestamp)?.toDate()
            ?: (a["analyzedAt"] as? Date)

        // Texto
        tvInterpretacion.text = "Interpretación: " + (if (interpretacion.isBlank()) "—" else interpretacion)
        tvRecomendacion.text  = "Recomendación: "  + (if (recomendacion.isBlank()) "—" else recomendacion)
        tvPrecision.text      = "Precisión de IA: " + (prec?.let { "${df1.format(it)}%" } ?: "—")
        tvRiesgo.text         = "Nivel de riesgo: " + (riesgo ?: "—")

        tvParametros.text = buildString {
            append("FC: ");  append(fc?.let { df0.format(it) } ?: "—"); append("  ")
            append("Ritmo: ");append(ritmo ?: "—"); append("  ")
            append("PR: ");  append(pr ?.let { df1.format(it) } ?: "—"); append(" ms  ")
            append("QRS: "); append(qrs?.let { df1.format(it) } ?: "—"); append(" ms  ")
            append("QT: ");  append(qt ?.let { df1.format(it) } ?: "—"); append(" ms  ")
            append("QTc: "); append(qtc?.let { df1.format(it) } ?: "—"); append(" ms")
        }

        // Estilos según riesgo
        styleRiskChip(riesgo)
        binding.chipAnalizado.isVisible = analyzedAt != null
    }

    private fun styleRiskChip(risk: String?) = with(binding) {
        chipRiesgo.isVisible = !risk.isNullOrBlank()
        if (risk.isNullOrBlank()) return@with

        val r = risk.lowercase(Locale.getDefault())
        val (bg, fg) = when (r) {
            "alto" -> R.drawable.bg_chip_red to android.R.color.white
            "moderado" -> R.drawable.bg_chip_orange to R.color.chip_orange_text
            else -> R.drawable.bg_chip_green to R.color.chip_green_text
        }
        chipRiesgo.setBackgroundResource(bg)
        chipRiesgo.setTextColor(ContextCompat.getColor(this@EcgDetailActivity, fg))
        chipRiesgo.text = risk
    }

    companion object {
        fun intent(c: android.content.Context, id: String) =
            android.content.Intent(c, EcgDetailActivity::class.java).putExtra("patient_id", id)
    }
}
