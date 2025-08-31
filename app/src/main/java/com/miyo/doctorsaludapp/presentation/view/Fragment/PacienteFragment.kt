package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.miyo.doctorsaludapp.databinding.FragmentPacienteBinding
import com.miyo.doctorsaludapp.presentation.view.Activity.PacienteActivity
import com.miyo.doctorsaludapp.presentation.view.Adapter.PacienteAdapter
import com.miyo.doctorsaludapp.presentation.viewmodel.PacientesViewModel

class PacienteFragment : Fragment(){
    private var _binding: FragmentPacienteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PacientesViewModel by viewModels()
    private lateinit var adapter: PacienteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacienteBinding.inflate(inflater, container, false)
    binding.btnAgregarPaciente.setOnClickListener {
        val intent = Intent(context, PacienteActivity::class.java)
        startActivity(intent)
    }
            return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PacienteAdapter(listOf(), viewModel)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        viewModel.pacientes.observe(viewLifecycleOwner) { pacientes ->
            adapter.updatePacientes(pacientes)
        }
        viewModel.getPacientes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}