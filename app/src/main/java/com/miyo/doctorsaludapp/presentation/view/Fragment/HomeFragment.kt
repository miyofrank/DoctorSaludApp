package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.miyo.doctorsaludapp.databinding.FragmentHomeBinding
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.presentation.adapters.RecentPatientsAdapter
import com.miyo.doctorsaludapp.presentation.view.Activity.PatientDetailActivity
import com.miyo.doctorsaludapp.presentation.view.Activity.RegisterPatientActivity
import com.miyo.doctorsaludapp.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val vm: HomeViewModel by viewModels()

    private lateinit var recentAdapter: RecentPatientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lista de recientes
        recentAdapter = RecentPatientsAdapter(onClick = ::goPatientDetail)
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = recentAdapter

        // Acciones rápidas
        binding.cardNewPatient.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterPatientActivity::class.java))
        }
        binding.cardAnalyzeEcg.setOnClickListener {
            // Si tienes NavGraph con destino Estadistica
            try {

                startActivity(Intent(requireContext(), com.miyo.doctorsaludapp.presentation.view.activity.StatsActivity::class.java))

            } catch (_: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir Análisis", Toast.LENGTH_SHORT).show()
            }
        }

        observe()
        vm.load()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    binding.homeProgress.isVisible = s.loading
                    binding.homeContent.isVisible = !s.loading

                    // Header
                    binding.tvHola.text = "Hola, Dr. ${s.doctorName}"
                    binding.tvSubHola.text = "${s.totalPacientes} pacientes registrados"

                    // KPIs
                    binding.tvKpiTotalPatients.text = s.totalPacientes.toString()
                    binding.tvKpiIaPrecision.text = s.iaPrecisionPct?.let {
                        String.format(Locale.getDefault(), "%.1f%%", it)
                    } ?: "—"

                    // Performance
                    binding.tvPerfAvgTime.text = s.iaAvgSeconds?.let {
                        String.format(Locale.getDefault(), "%.1fs", it)
                    } ?: "—"
                    binding.tvPerfSaved.text = s.iaSavedMinutes?.let {
                        String.format(Locale.getDefault(), "%.1f min", it)
                    } ?: "—"

                    // Riesgo chips
                    binding.tvRiskBajo.text = "Bajo (${s.riskBajo})"
                    binding.tvRiskModerado.text = "Moderado (${s.riskModerado})"
                    binding.tvRiskAlto.text = "Alto (${s.riskAlto})"

                    // Recientes
                    recentAdapter.submit(s.recientes)

                    s.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun goPatientDetail(p: Patient) {
        startActivity(
            Intent(requireContext(), PatientDetailActivity::class.java)
                .putExtra("patient_id", p.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


