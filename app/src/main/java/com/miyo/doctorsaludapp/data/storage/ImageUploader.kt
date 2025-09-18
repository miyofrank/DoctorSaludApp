package com.miyo.doctorsaludapp.data.storage

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class ImageUploader {
    /**
     * Sube el ECG a Storage en ecgs/{patientId}/{timestamp}.(png|jpg)
     * Devuelve (downloadUrl, ecgId, mime).
     */
    suspend fun uploadEcg(patientId: String, inputUri: Uri, mime: String): Triple<String,String,String> {
        val ecgId = if (mime == "image/png") "${System.currentTimeMillis()}.png"
        else "${System.currentTimeMillis()}.jpg"
        val path = "ecgs/$patientId/$ecgId"
        val ref = Firebase.storage.reference.child(path)

        val md = StorageMetadata.Builder().setContentType(mime).build()
        ref.putFile(inputUri, md).await()
        val url = ref.downloadUrl.await().toString()
        return Triple(url, ecgId, mime)
    }
}
