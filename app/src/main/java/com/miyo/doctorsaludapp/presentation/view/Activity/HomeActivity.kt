package com.miyo.doctorsaludapp.presentation.view.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.databinding.ActivityHomeBinding
import com.miyo.doctorsaludapp.presentation.view.Adapter.ViewPagerAdapter
import com.miyo.doctorsaludapp.presentation.view.Fragment.AnalisisFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.HomeFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.PacienteFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.PerfilFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: ViewPagerAdapter
    private var isUserClick = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preparamos el ViewPager con 4 fragments
        val fragments = listOf(
            HomeFragment(),
            PacienteFragment(),
            AnalisisFragment(),
            PerfilFragment()
        )

        adapter = ViewPagerAdapter(this, fragments)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = fragments.size
        binding.viewPager.isUserInputEnabled = true // swipes habilitados

        // Sincronizamos BottomNav -> ViewPager
        binding.bottomNav.setOnItemSelectedListener { item ->
            isUserClick = true
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.currentItem = 0
                R.id.nav_pacientes -> binding.viewPager.currentItem = 1
                R.id.nav_analisis -> binding.viewPager.currentItem = 2
                R.id.nav_perfil -> binding.viewPager.currentItem = 3
            }
            true
        }

        // Página inicial
        binding.bottomNav.selectedItemId = R.id.nav_home

        // Sincronizamos ViewPager -> BottomNav
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (!isUserClick) {
                    val menu = binding.bottomNav.menu
                    menu.getItem(position).isChecked = true
                }
                isUserClick = false
            }
        })
    }
}
