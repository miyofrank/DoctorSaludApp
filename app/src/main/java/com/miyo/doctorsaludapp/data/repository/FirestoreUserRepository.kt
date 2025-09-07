package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.miyo.doctorsaludapp.domain.model.UserProfile
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val db: FirebaseFirestore,
    private val collection: String = "usuarios" // usa "usuarios"; si tu colección se llama "users", cámbialo aquí.
) {

    suspend fun getById(uid: String): UserProfile? {
        val snap = db.collection(collection).document(uid).get().await()
        return if (snap.exists()) snap.toObject(UserProfile::class.java)?.apply { id = uid } else null
    }

    suspend fun set(uid: String, profile: UserProfile) {
        val data = profile.copy(id = uid, updatedAt = System.currentTimeMillis())
        db.collection(collection).document(uid).set(data, SetOptions.merge()).await()
    }

    suspend fun updateAutoAnalysis(uid: String, enabled: Boolean) {
        db.collection(collection).document(uid)
            .set(mapOf("autoAnalisis" to enabled, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
    }
}
