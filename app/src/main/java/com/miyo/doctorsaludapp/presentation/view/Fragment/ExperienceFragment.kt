package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.miyo.doctorsaludapp.databinding.FragmentExperienceBinding
import com.miyo.doctorsaludapp.presentation.view.Activity.HomeActivity
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModel

class ExperienceFragment : Fragment() {

    private lateinit var binding: FragmentExperienceBinding
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExperienceBinding.inflate(inflater, container, false)

        val email = arguments?.getString("email")
        val password = arguments?.getString("password")
        val firstName = arguments?.getString("firstName")
        val lastName = arguments?.getString("lastName")
        val specialization = arguments?.getString("specialization")

        binding.finishButton.setOnClickListener {
            val experienceYears = binding.experienceEditText.text.toString().toInt()
        }

        authViewModel.user.observe(viewLifecycleOwner, { user ->
            user?.let {
                // Manejar el registro exitoso
                // Navegar a HomeActivity
                val intent = Intent(requireActivity(), HomeActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        })

        authViewModel.error.observe(viewLifecycleOwner, { error ->
            error?.let {
                // Manejar el error
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root
    }
}
