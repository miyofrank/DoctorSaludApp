package com.miyo.doctorsaludapp.presentation.view.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.miyo.doctorsaludapp.databinding.FragmentImagenAiBinding
import com.miyo.doctorsaludapp.domain.usecase.ReportaDiagnosticoPreoperatorioUseCase
import java.io.File
import java.io.IOException

class ImagenAiFragment : Fragment() {

    private var _binding: FragmentImagenAiBinding? = null
    private val binding get() = _binding!!
    private lateinit var storageReference: StorageReference
    private lateinit var fileUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImagenAiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storageReference = FirebaseStorage.getInstance().reference

        binding.selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            fileUri = result.data!!.data!!

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, fileUri)
                binding.imageView.setImageBitmap(bitmap)
                uploadImage()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        val ref = storageReference.child("images/${System.currentTimeMillis()}.jpg")
        val uploadTask = ref.putFile(fileUri)
        uploadTask.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                downloadAndProcessImage(uri)
            }
        }.addOnFailureListener {
            // Manejar fallos
        }
    }

    private fun downloadAndProcessImage(uri: Uri) {
        val localFile = File.createTempFile("tempImage", "jpg")
        storageReference.child(uri.lastPathSegment!!).getFile(localFile).addOnSuccessListener {
            val localUri = Uri.fromFile(localFile)
            val imageProcessor = ReportaDiagnosticoPreoperatorioUseCase(requireContext())
            imageProcessor.processImage(localUri) { result ->
                binding.resultTextView.text = result
            }
        }.addOnFailureListener {
            // Manejar fallos
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}