package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.databinding.FragmentSpecializationBinding

class SpecializationFragment : Fragment() {

    private lateinit var binding: FragmentSpecializationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSpecializationBinding.inflate(inflater, container, false)

        val email = arguments?.getString("email")
        val password = arguments?.getString("password")
        val firstName = arguments?.getString("firstName")
        val lastName = arguments?.getString("lastName")

        binding.specializationButton.setOnClickListener {
            val specialization = binding.specializationEditText.text.toString()

            val bundle = Bundle().apply {
                putString("email", email)
                putString("password", password)
                putString("firstName", firstName)
                putString("lastName", lastName)
                putString("specialization", specialization)
            }

            val fragment = ExperienceFragment()
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }
}
