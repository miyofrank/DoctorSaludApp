package com.miyo.doctorsaludapp.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import com.miyo.doctorsaludapp.domain.repository.StorageRepository
import kotlinx.coroutines.tasks.await

class FirebaseStorageRepository(
    private val storage: FirebaseStorage,
    private val contentResolver: ContentResolver
) : StorageRepository {

    override suspend fun uploadSingle(uri: Uri, destPath: String): String {
        val ref = storage.reference.child(destPath)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun uploadMultiple(uris: List<Uri>, basePath: String): List<String> {
        val urls = mutableListOf<String>()
        for (u in uris) {
            val name = buildUniqueName(u)
            val path = "$basePath/$name"
            val ref = storage.reference.child(path)
            ref.putFile(u).await()
            urls.add(ref.downloadUrl.await().toString())
        }
        return urls
    }

    // ------- helpers -------
    private fun buildUniqueName(uri: Uri): String {
        val ext = getExt(uri).ifEmpty { "bin" }
        return "file_${System.currentTimeMillis()}.$ext"
    }

    private fun getExt(uri: Uri): String {
        val mime = contentResolver.getType(uri)
        val guess = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        return guess ?: uri.lastPathSegment?.substringAfterLast('.', "") ?: ""
    }
}
