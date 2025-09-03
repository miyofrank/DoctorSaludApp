package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.miyo.doctorsaludapp.databinding.ActivityPacienteBinding
import com.miyo.doctorsaludapp.presentation.viewmodel.RegisterViewModel

class PacienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPacienteBinding
    private val viewModel: RegisterViewModel by viewModels()
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityPacienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileImageView.setOnClickListener {
            openFileChooser()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val surname = binding.etSurname.text.toString()
            val email = binding.etEmail.text.toString()
            val address = binding.etAddress.text.toString()
            val ageText = binding.etAge.text.toString()
            val age: Int = if (ageText.isNotEmpty()) ageText.toInt() else 0
            val date = binding.etDate.text.toString()
            val gender = binding.etGender.text.toString()
            val phoneText = binding.etPhone.text.toString()
            val phone: Int = if (phoneText.isNotEmpty()) phoneText.toInt() else 0
            viewModel.registerPatient(name, surname, email, address, age, date, gender, phone, imageUri)
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
}
