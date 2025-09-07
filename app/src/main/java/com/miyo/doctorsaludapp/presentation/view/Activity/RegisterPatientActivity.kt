package com.miyo.doctorsaludapp.presentation.view.Activity

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirebaseStorageRepository
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.databinding.ActivityRegisterPatientBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.patient.SetPatientUseCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterPatientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPatientBinding

    private val ecgPicker = 101
    private val examenesPicker = 102
    private var ecgUri: Uri? = null
    private val examenesUris = mutableListOf<Uri>()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Repos
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val patientRepo by lazy { FirestorePatientRepository(firestore, "pacientes") }
    private val storageRepo by lazy { FirebaseStorageRepository(storage, contentResolver) }
    private val setPatientUseCase by lazy { SetPatientUseCase(patientRepo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        setupPickers()
        setupActions()
    }

    private fun setupDropdowns() {
        binding.ddSexo.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.sexo_options)))
        binding.ddGrupo.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.grupo_sanguineo_options)))
        binding.ddAnestesia.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.anestesia_options)))
        binding.ddUrgencia.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.urgencia_options)))
    }

    private fun setupPickers() {
        binding.etFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(y, m, d, 0, 0, 0)
                    binding.etFecha.setText(dateFormat.format(cal.time))
                    binding.etFecha.tag = cal.time
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnPickEcg.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
                flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivityForResult(intent, ecgPicker)
        }

        binding.btnPickExamenes.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivityForResult(intent, examenesPicker)
        }
    }

    private fun setupActions() {
        binding.btnCancelar.setOnClickListener { finish() }
        binding.btnRegistrar.setOnClickListener { savePatientWithUploads() }
    }

    private fun String.toListByComma(): List<String>? =
        this.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { null }

    private fun buildPatientPartial(): Patient? {
        val dni = binding.etDni.text?.toString()?.trim().orEmpty()
        val nombres = binding.etNombres.text?.toString()?.trim().orEmpty()
        val apellidos = binding.etApellidos.text?.toString()?.trim().orEmpty()
        val edad = binding.etEdad.text?.toString()?.toIntOrNull()
        val sexo = binding.ddSexo.text?.toString()?.trim().orEmpty()

        if (dni.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || edad == null || sexo.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios (*)", Toast.LENGTH_SHORT).show()
            return null
        }

        return Patient(
            dni = dni,
            nombres = nombres,
            apellidos = apellidos,
            nombreCompleto = "$nombres $apellidos".trim(),
            edad = edad,
            sexo = sexo,
            grupoSanguineo = binding.ddGrupo.text?.toString()?.trim(),
            alturaCm = binding.etAltura.text?.toString()?.toIntOrNull(),
            pesoKg = binding.etPeso.text?.toString()?.toIntOrNull(),

            alergias = binding.etAlergias.text?.toString()?.trim()?.toListByComma(),
            medicamentosActuales = binding.etMedicamentos.text?.toString()?.trim()?.toListByComma(),
            enfermedadesCronicas = binding.etCronicas.text?.toString()?.trim()?.toListByComma(),
            cirugiasPrevias = binding.etCirugiasPrevias.text?.toString()?.trim()?.toListByComma(),
            antecedentesFamiliares = binding.etAntecedentesFam.text?.toString()?.trim()?.toListByComma(),

            tipoCirugia = binding.etTipoCirugia.text?.toString()?.trim(),
            fechaCirugia = binding.etFecha.tag as? Date,
            duracionEstimadaMin = binding.etDuracion.text?.toString()?.toIntOrNull(),
            tipoAnestesia = binding.ddAnestesia.text?.toString()?.trim(),
            urgencia = binding.ddUrgencia.text?.toString()?.trim(),
            cirujano = binding.etCirujano.text?.toString()?.trim(),

            examenesTexto = binding.etExamenesTexto.text?.toString()?.trim(),
            notas = binding.etNotas.text?.toString()?.trim(),

            estado = "En evaluación",
            riesgo = null,
            riesgoPct = null,

            createdAt = Date(),
            updatedAt = Date()
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegistrar.isEnabled = !loading
        binding.btnRegistrar.alpha = if (loading) 0.6f else 1f
    }

    private fun savePatientWithUploads() {
        val partial = buildPatientPartial() ?: return
        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1) Generar ID de documento
                val docRef = firestore.collection("pacientes").document()
                val id = docRef.id
                val basePath = "patients/$id"

                // 2) Subir archivos a Storage
                var ecgUrl: String? = null
                var ecgName: String? = null
                var ecgMime: String? = null

                ecgUri?.let { uri ->
                    // mantener permiso persistente
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { /* ignore */ }

                    val name = "ecg_${System.currentTimeMillis()}"
                    val path = "$basePath/ecg/$name"
                    ecgUrl = storageRepo.uploadSingle(uri, path)
                    ecgName = name
                    ecgMime = contentResolver.getType(uri)
                }

                val examUrls: List<String>? = if (examenesUris.isNotEmpty()) {
                    // persistir permisos
                    examenesUris.forEach { u ->
                        try {
                            contentResolver.takePersistableUriPermission(
                                u,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) { }
                    }
                    storageRepo.uploadMultiple(examenesUris, "$basePath/exams")
                } else null

                // 3) Completar paciente con URLs y guardar en Firestore (set por ID)
                val patient = partial.copy(
                    id = id,
                    ecgUrl = ecgUrl,
                    ecgId = ecgName,
                    ecgMime = ecgMime,
                    examenesArchivos = examUrls
                )

                setPatientUseCase(id, patient)

                Toast.makeText(this@RegisterPatientActivity, "Paciente registrado", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterPatientActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            ecgPicker -> {
                ecgUri = data.data
                binding.tvEcgFile.text = ecgUri?.lastPathSegment ?: "Archivo seleccionado"
            }
            examenesPicker -> {
                examenesUris.clear()
                data.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) {
                        examenesUris.add(clip.getItemAt(i).uri)
                    }
                } ?: data.data?.let { single -> examenesUris.add(single) }

                binding.tvExamenesFiles.text =
                    if (examenesUris.isEmpty()) "Sin archivos" else "${examenesUris.size} archivo(s) seleccionado(s)"
            }
        }
    }
}
