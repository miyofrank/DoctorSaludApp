package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.databinding.FragmentExperienceBinding
import com.miyo.doctorsaludapp.databinding.FragmentHomeBinding
import com.miyo.doctorsaludapp.presentation.view.Adapter.BannerAdapter
import com.miyo.doctorsaludapp.presentation.viewmodel.BannerViewModel
import dagger.hilt.android.AndroidEntryPoint

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BannerViewModel by viewModels()
    private lateinit var adapter: BannerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        adapter = BannerAdapter(listOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.banners.observe(viewLifecycleOwner) { banners ->
            adapter.updateBanners(banners)
        }

        viewModel.getBanners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
