package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.miyo.doctorsaludapp.BuildConfig

object GeminiClient {
    /**
     * Usa la clave del BuildConfig (debe ser un String no nulo).
     * Si tu campo no existe, define en build.gradle (app):
     * buildConfigField "String", "GOOGLE_AI_API_KEY", "\"TU_API_KEY\""
     */
    fun get(context: Context): GenerativeModel {
        val key: String = BuildConfig.GOOGLE_AI_API_KEY  // <- NO nulo
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key
        )
    }
}
