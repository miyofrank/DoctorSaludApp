package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.miyo.doctorsaludapp.BuildConfig

object GeminiClient {

    // IMPORTANTE: define GOOGLE_AI_API_KEY en build.gradle (BuildConfig field)
    // buildConfigField("String", "GOOGLE_AI_API_KEY", "\"TU_API_KEY\"")

    fun get(@Suppress("UNUSED_PARAMETER") context: Context): GenerativeModel {
        val key = "AIzaSyAAlTD0VvuBjzFJRd5UL9QboNcaeg4L4KQ"
        require(!key.isNullOrBlank() && key.length > 20) {
            "API Key de Gemini inválida o vacía (BuildConfig.GOOGLE_AI_API_KEY)."
        }
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key
        )
    }
}
