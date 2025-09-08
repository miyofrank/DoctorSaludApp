package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.databinding.ActivityPatientDetailBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.patient.DeletePatientUseCase
import com.miyo.doctorsaludapp.domain.usecase.patient.GetPatientByIdUseCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding

    private val repo by lazy { FirestorePatientRepository(FirebaseFirestore.getInstance(), "pacientes") }
    private val getById by lazy { GetPatientByIdUseCase(repo) }
    private val deleteUse by lazy { DeletePatientUseCase(repo) }

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var patientId: String? = null
    private var patient: Patient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        patientId = intent.getStringExtra("patient_id")
        if (patientId.isNullOrEmpty()) {
            Toast.makeText(this, "ID de paciente no válido", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        setupToolbar()
        loadPatient()
        setupActions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupActions() {
        binding.btnVerEcg.setOnClickListener {
            startActivity(Intent(this, EcgDetailActivity::class.java).putExtra("patient_id", patientId))
        }
        binding.btnEditar.setOnClickListener {
            startActivity(Intent(this, RegisterPatientActivity::class.java).putExtra("patient_id", patientId))
        }
    }

    private fun loadPatient() {
        lifecycleScope.launch {
            val p = getById(patientId!!)
            if (p == null) {
                Toast.makeText(this@PatientDetailActivity, "Paciente no encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            patient = p
            bindPatient(p)
        }
    }

    private fun listToText(list: List<String>?): String = list?.joinToString(", ").orEmpty()

    private fun bindPatient(p: Patient) = with(binding) {
        val nombre = when {
            !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
            else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
        }
        tvNombre.text = nombre
        tvDni.text = "DNI: ${p.dni.orEmpty()}"

        // estado chip
        val estado = (p.estado ?: "").lowercase(Locale.getDefault())
        when {
            estado.contains("apto") || estado.contains("aproba") -> {
                chipEstado.text = if (estado.contains("aproba")) "Aprobado" else "Apto"
                chipEstado.setBackgroundResource(R.drawable.bg_chip_green)
                chipEstado.setTextColor(getColor(R.color.chip_green_text))
            }
            estado.contains("evalu") -> {
                chipEstado.text = "En evaluación"
                chipEstado.setBackgroundResource(R.drawable.bg_chip_orange)
                chipEstado.setTextColor(getColor(R.color.chip_orange_text))
            }
            estado.contains("rech") || estado.contains("no apto") -> {
                chipEstado.text = "No apto"
                chipEstado.setBackgroundResource(R.drawable.bg_chip_red)
                chipEstado.setTextColor(getColor(R.color.white))
            }
            else -> {
                chipEstado.text = p.estado?.takeIf { it.isNotBlank() } ?: "-"
                chipEstado.setBackgroundResource(R.drawable.bg_chip_orange)
                chipEstado.setTextColor(getColor(R.color.chip_orange_text))
            }
        }

        tvEdad.text  = "Edad: ${p.edad?.toString() ?: ""} años"
        tvSexo.text  = "Sexo: ${p.sexo.orEmpty()}"
        tvAltura.text= "Altura: ${p.alturaCm?.toString() ?: ""} cm"
        tvPeso.text  = "Peso: ${p.pesoKg?.toString() ?: ""} kg"

        tvTipoCirugia.text = "Tipo: ${p.tipoCirugia.orEmpty()}"
        tvFechaCirugia.text = "Fecha: ${p.fechaCirugia?.let { df.format(it) } ?: ""}"
        tvCirujano.text = "Cirujano: ${p.cirujano.orEmpty()}"
        tvDuracion.text = "Duración estimada: ${p.duracionEstimadaMin?.toString() ?: ""} min"

        tvCronicas.text = "Enfermedades crónicas: ${listToText(p.enfermedadesCronicas)}"
        tvMedicamentos.text = "Medicamentos actuales: ${listToText(p.medicamentosActuales)}"
        tvAlergias.text = "Alergias: ${listToText(p.alergias)}"
        tvCirugiasPrevias.text = "Cirugías previas: ${listToText(p.cirugiasPrevias)}"
        tvAntecedentesFam.text = "Antecedentes familiares: ${listToText(p.antecedentesFamiliares)}"
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar paciente")
            .setMessage("¿Deseas eliminar este paciente? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _: DialogInterface, _: Int ->
                doDelete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doDelete() {
        val id = patientId ?: return
        lifecycleScope.launch {
            try {
                deleteUse(id)
                Toast.makeText(this@PatientDetailActivity, "Paciente eliminado", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@PatientDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
