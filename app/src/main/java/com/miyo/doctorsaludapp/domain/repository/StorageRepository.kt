package com.miyo.doctorsaludapp.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadSingle(uri: Uri, destPath: String): String  // -> download URL
    suspend fun uploadMultiple(uris: List<Uri>, basePath: String): List<String> // nombres únicos dentro de basePath
}
