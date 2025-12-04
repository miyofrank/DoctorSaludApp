package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.FirestoreUserRepository
import com.miyo.doctorsaludapp.databinding.FragmentPerfilBinding
import com.miyo.doctorsaludapp.domain.model.UserProfile
import com.miyo.doctorsaludapp.domain.usecase.user.GetUserProfileUseCase
import com.miyo.doctorsaludapp.domain.usecase.user.UpdateAutoAnalysisPrefUseCase
import com.miyo.doctorsaludapp.domain.usecase.user.UpdateUserProfileUseCase
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { FirestoreUserRepository(FirebaseFirestore.getInstance()) }
    private val getUse by lazy { GetUserProfileUseCase(repo) }
    private val setUse by lazy { UpdateUserProfileUseCase(repo) }
    private val setAutoUse by lazy { UpdateAutoAnalysisPrefUseCase(repo) }

    private var original: UserProfile? = null
    private var editMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfile()
        setupUi()
    }

    private fun setupUi() = with(binding) {
        btnEdit.setOnClickListener { if (!editMode) enterEdit() else saveProfile() }
        btnCancelar.setOnClickListener { leaveEdit(discard = true) }
        btnGuardar.setOnClickListener { saveProfile() }
        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            val uid = auth.currentUser?.uid ?: return@setOnCheckedChangeListener
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    setAutoUse(uid, isChecked)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_LONG).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Lee de `usuarios`; si no existe, intenta fallbacks y migra
                val prof = getUse(uid) ?: UserProfile(id = uid, email = auth.currentUser?.email)
                original = prof
                bind(prof)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bind(p: UserProfile) = with(binding) {
        etNombres.setText(p.nombres.orEmpty())
        etApellidos.setText(p.apellidos.orEmpty())
        etEmail.setText(p.email.orEmpty())
        etEspecialidad.setText(p.especialidad.orEmpty())
        etHospital.setText(p.hospital.orEmpty())
        etColegiatura.setText(p.colegiatura.orEmpty())
        etTelefono.setText(p.telefono.orEmpty())
        switchAuto.isChecked = p.autoAnalisis == true
        disableInputs(true)
        actionsEdit.visibility = View.GONE
        editMode = false
        btnEdit.text = "Editar"
    }

    private fun enterEdit() {
        editMode = true
        binding.btnEdit.text = "Guardar"
        binding.actionsEdit.visibility = View.VISIBLE
        disableInputs(false)
        binding.etEmail.isEnabled = false // email solo lectura
    }

    private fun leaveEdit(discard: Boolean) {
        if (discard) original?.let { bind(it) } else disableInputs(true)
        binding.actionsEdit.visibility = View.GONE
        editMode = false
        binding.btnEdit.text = "Editar"
    }

    private fun disableInputs(disable: Boolean) = with(binding) {
        etNombres.isEnabled = !disable
        etApellidos.isEnabled = !disable
        etEspecialidad.isEnabled = !disable
        etHospital.isEnabled = !disable
        etColegiatura.isEnabled = !disable
        etTelefono.isEnabled = !disable
        // etEmail queda deshabilitado
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val updated = (original ?: UserProfile(id = uid, email = auth.currentUser?.email)).copy(
            nombres = binding.etNombres.text?.toString()?.trim(),
            apellidos = binding.etApellidos.text?.toString()?.trim(),
            especialidad = binding.etEspecialidad.text?.toString()?.trim(),
            hospital = binding.etHospital.text?.toString()?.trim(),
            colegiatura = binding.etColegiatura.text?.toString()?.trim(),
            telefono = binding.etTelefono.text?.toString()?.trim(),
            autoAnalisis = binding.switchAuto.isChecked
        )
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                setUse(uid, updated)
                original = updated
                bind(updated)
                Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
