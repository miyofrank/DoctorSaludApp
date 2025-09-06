package com.miyo.doctorsaludapp.data.remote

import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseService {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    suspend fun login(email: String, password: String): AuthResult {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun register(email: String, password: String, firstName: String, lastName: String, especializacion: String, hospital: String, licencia: String): FirebaseUser? {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Guardar datos adicionales en Firestore
                val userMap = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "especializacion" to especializacion,
                    "hospital" to hospital,
                    "licencia" to licencia
                )
                firestore.collection("users").document(user.uid).set(userMap).await()
            }
            result.user
        } catch (e: Exception) {
            null
        }
    }
}