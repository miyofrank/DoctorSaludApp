package com.miyo.doctorsaludapp.presentation.view.Activity

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.databinding.ActivityHomeBinding
import com.miyo.doctorsaludapp.presentation.view.Adapter.ViewPagerAdapter
import com.miyo.doctorsaludapp.presentation.view.Fragment.AnalisisFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.ChatFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.HomeFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.ImagenAiFragment
import com.miyo.doctorsaludapp.presentation.view.Fragment.PacienteFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var currentSelectedLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragments = listOf(
            HomeFragment(),
            AnalisisFragment(),
            PacienteFragment(),
            ImagenAiFragment(),
            ChatFragment()
        )

        val viewPagerAdapter = ViewPagerAdapter(this, fragments)
        binding.viewPager.adapter = viewPagerAdapter

        val tabLayout = binding.tabLayout
        TabLayoutMediator(tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Home"
                1 -> tab.text = "Analisis"
                2 -> tab.text = "Paciente"
                3 -> tab.text = "Imagen AI"
                4 -> tab.text = "Chat"
            }
        }.attach()

        binding.layoutHome.setOnClickListener { selectPage(0, binding.layoutHome) }
        binding.layoutAnalisis.setOnClickListener { selectPage(1, binding.layoutAnalisis) }
        binding.layoutPaciente.setOnClickListener { selectPage(2, binding.layoutPaciente) }
        binding.layoutImagenAi.setOnClickListener { selectPage(3, binding.layoutImagenAi) }
        binding.layoutChat.setOnClickListener { selectPage(4, binding.layoutChat) }
    }

    private fun selectPage(pageIndex: Int, selectedLayout: LinearLayout) {
        binding.viewPager.currentItem = pageIndex
        if (currentSelectedLayout != selectedLayout) {
            currentSelectedLayout?.isSelected = false
            currentSelectedLayout = selectedLayout
            animateLayout(selectedLayout)
        }
    }

    private fun animateLayout(layout: LinearLayout) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.jump_up)
        val imageView: ImageView? = when (layout.id) {
            R.id.layout_home -> layout.findViewById(R.id.idhome)
            R.id.layout_analisis -> layout.findViewById(R.id.idanalisis)
            R.id.layout_paciente -> layout.findViewById(R.id.idpaciente)
            R.id.layout_imagen_ai -> layout.findViewById(R.id.idprocedimientoimagenes)
            R.id.layout_chat -> layout.findViewById(R.id.idchat)
            else -> null
        }

        imageView?.startAnimation(animation)
        layout.isSelected = true
    }
}
