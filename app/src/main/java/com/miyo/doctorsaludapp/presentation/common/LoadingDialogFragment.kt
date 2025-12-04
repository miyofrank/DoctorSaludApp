package com.miyo.doctorsaludapp.presentation.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.miyo.doctorsaludapp.databinding.DialogLoadingBinding

/**
 * Diálogo de carga bloqueante, seguro ante rotación y re-creación.
 * Usa viewBinding sobre dialog_loading.xml
 */
class LoadingDialogFragment : DialogFragment() {

    private var _binding: DialogLoadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogLoadingBinding.inflate(LayoutInflater.from(requireContext()))

        val message = arguments?.getString(ARG_MESSAGE) ?: "Cargando..."
        binding.message.text = message

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        // Si quieres sin oscurecer detrás, descomenta:
        // dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LoadingDialog"
        private const val ARG_MESSAGE = "arg_message"

        fun newInstance(message: String): LoadingDialogFragment {
            return LoadingDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
