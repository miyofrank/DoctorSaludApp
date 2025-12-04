package com.miyo.doctorsaludapp.presentation.common

import androidx.fragment.app.FragmentManager

/**
 * Helper para mostrar/ocultar el LoadingDialogFragment de forma simple y segura.
 */
object LoadingDialog {

    /**
     * Muestra el diálogo de carga si no está ya visible.
     * @param fm FragmentManager de la Activity o Fragment
     * @param message Mensaje a mostrar (por defecto "Cargando...")
     */
    fun show(fm: FragmentManager, message: String = "Cargando...") {
        val existing = fm.findFragmentByTag(LoadingDialogFragment.TAG) as? LoadingDialogFragment
        if (existing?.dialog?.isShowing == true || existing?.isAdded == true) return

        // Evita IllegalStateException si ya se guardó el estado
        if (fm.isStateSaved) {
            fm.beginTransaction()
                .add(LoadingDialogFragment.newInstance(message), LoadingDialogFragment.TAG)
                .commitAllowingStateLoss()
        } else {
            LoadingDialogFragment.newInstance(message)
                .show(fm, LoadingDialogFragment.TAG)
        }
    }

    /**
     * Oculta el diálogo si está visible.
     */
    fun hide(fm: FragmentManager) {
        val existing = fm.findFragmentByTag(LoadingDialogFragment.TAG) as? LoadingDialogFragment
        existing?.dismissAllowingStateLoss()
    }
}
