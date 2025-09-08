package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.miyo.doctorsaludapp.domain.model.UserProfile
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val db: FirebaseFirestore
) {
    private val primary = "usuarios"
    private val fallbacks = listOf("users", "medicos")

    /** Lee de `usuarios/{uid}`; si no existe, intenta en fallbacks y migra a `usuarios/{uid}`. */
    suspend fun getById(uid: String): UserProfile? {
        // 1) primary
        db.collection(primary).document(uid).get().await().let { snap ->
            if (snap.exists()) return snap.toObject(UserProfile::class.java)?.apply { id = uid }
        }
        // 2) fallbacks
        for (c in fallbacks) {
            val s = db.collection(c).document(uid).get().await()
            if (s.exists()) {
                val p = s.toObject(UserProfile::class.java)?.apply { id = uid }
                if (p != null) {
                    // migrar a primary
                    set(uid, p)
                    return p
                }
            }
        }
        return null
    }

    suspend fun set(uid: String, profile: UserProfile) {
        val now = System.currentTimeMillis()
        val payload = profile.copy(id = uid, updatedAt = now).let {
            if (it.createdAt == null) it.copy(createdAt = now) else it
        }
        db.collection(primary).document(uid)
            .set(payload, SetOptions.merge()).await()
    }

    suspend fun updateFields(uid: String, map: Map<String, Any?>) {
        db.collection(primary).document(uid)
            .set(map + mapOf("updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
    }

    suspend fun updateAutoAnalysis(uid: String, enabled: Boolean) {
        updateFields(uid, mapOf("autoAnalisis" to enabled))
    }
}
