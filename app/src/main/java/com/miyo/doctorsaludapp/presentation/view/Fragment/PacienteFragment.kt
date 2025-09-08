package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirestorePatientRepository
import com.miyo.doctorsaludapp.databinding.FragmentPacienteBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.patient.GetPatientsUseCase
import com.miyo.doctorsaludapp.presentation.view.Activity.EcgDetailActivity
import com.miyo.doctorsaludapp.presentation.view.Activity.PatientDetailActivity
import com.miyo.doctorsaludapp.presentation.view.Activity.RegisterPatientActivity
import com.miyo.doctorsaludapp.presentation.view.Adapter.PacienteAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class PacienteFragment : Fragment() {

    private var _binding: FragmentPacienteBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PacienteAdapter
    private lateinit var getPatientsUseCase: GetPatientsUseCase

    private val all = mutableListOf<Patient>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPacienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = FirestorePatientRepository(FirebaseFirestore.getInstance(), "pacientes")
        getPatientsUseCase = GetPatientsUseCase(repo)

        adapter = PacienteAdapter(
            onVerDetalles = { p ->
                startActivity(
                    Intent(requireContext(), PatientDetailActivity::class.java)
                        .putExtra("patient_id", p.id)
                )
            },
            onVerEcg = { p ->
                startActivity(
                    Intent(requireContext(), EcgDetailActivity::class.java)
                        .putExtra("patient_id", p.id)
                )
            }
        )

        binding.rvPacientes.adapter = adapter
        setupFilters()
        setupActions()
        observePatients()
    }

    private fun setupFilters() {
        val estados = resources.getStringArray(R.array.filtro_estados)
        val riesgos = resources.getStringArray(R.array.filtro_riesgos)

        binding.etEstado.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados))
        binding.etRiesgo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, riesgos))

        binding.etEstado.setOnItemClickListener { _, _, _, _ -> applyFilters() }
        binding.etRiesgo.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener { applyFilters() }
        binding.fabAddPaciente.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterPatientActivity::class.java))
        }
    }

    private fun observePatients() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            getPatientsUseCase().collectLatest { list ->
                all.clear()
                all.addAll(list)
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val q = binding.etBuscar.text?.toString()?.trim().orEmpty().lowercase(Locale.getDefault())
        val fEstado = binding.etEstado.text?.toString()?.trim().orEmpty()
        val fRiesgo = binding.etRiesgo.text?.toString()?.trim().orEmpty()

        val result = all.filter { p ->
            val nombre = when {
                !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
                else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
            }.lowercase(Locale.getDefault())

            val byText = if (q.isEmpty()) true else
                nombre.contains(q) || (p.dni ?: "").lowercase(Locale.getDefault()).contains(q)

            val byEstado = when (fEstado) {
                "", "Todos los estados" -> true
                else -> (p.estado ?: "").contains(textToEstadoKey(fEstado), ignoreCase = true)
            }

            val byRiesgo = when (fRiesgo) {
                "", "Todos los riesgos" -> true
                else -> (p.riesgo ?: "").contains(textToRiesgoKey(fRiesgo), ignoreCase = true)
            }

            byText && byEstado && byRiesgo
        }

        adapter.submit(result)
        binding.emptyState.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
        binding.rvPacientes.visibility = if (result.isEmpty()) View.GONE else View.VISIBLE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun textToEstadoKey(text: String): String {
        val t = text.lowercase(Locale.getDefault())
        return when {
            t.contains("evalu") -> "evalu"
            t.contains("apto") -> "apto"
            t.contains("aproba") -> "aproba"
            t.contains("no apto") || t.contains("rech") -> "rech"
            else -> t
        }
    }

    private fun textToRiesgoKey(text: String): String {
        val t = text.lowercase(Locale.getDefault())
        return when {
            t.contains("alto") -> "alto"
            t.contains("moder") -> "moder"
            t.contains("bajo") -> "bajo"
            else -> t
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

