package com.miyo.doctorsaludapp.domain.model

data class UserProfile(
    var id: String? = null,             // uid de FirebaseAuth
    var nombres: String? = null,
    var apellidos: String? = null,
    var email: String? = null,
    var especialidad: String? = null,
    var hospital: String? = null,
    var colegiatura: String? = null,    // CMP
    var telefono: String? = null,
    var autoAnalisis: Boolean? = false, // preferencia
    var updatedAt: Long? = null
)
