package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.miyo.doctorsaludapp.R

/**
 * Lee la API key desde strings.xml (Opción C) y configura JSON mode
 * directamente en el modelo, así no necesitas pasarlo en cada llamada.
 */
object GeminiClient {
    @Volatile private var model: GenerativeModel? = null

    fun get(context: Context): GenerativeModel {
        return model ?: synchronized(this) {
            val apiKey = context.getString(R.string.google_ai_api_key)
            val cfg = generationConfig {
                // JSON mode global: todas las respuestas en application/json
                responseMimeType = "application/json"
            }
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = cfg
            ).also { model = it }
        }
    }
}
