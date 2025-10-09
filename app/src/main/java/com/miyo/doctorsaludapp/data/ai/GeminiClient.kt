package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel

object GeminiClient {

    // IMPORTANTE: define GOOGLE_AI_API_KEY en build.gradle (BuildConfig field)
    // buildConfigField("String", "GOOGLE_AI_API_KEY", "\"TU_API_KEY\"")

    fun get(@Suppress("UNUSED_PARAMETER") context: Context): GenerativeModel {
        val key = "AIzaSyBMI--OSZNa8ZrpPLg6xOMVQH6caIBX9FM"
        require(!key.isNullOrBlank() && key.length > 20) {
            "API Key de Gemini inválida o vacía (BuildConfig.GOOGLE_AI_API_KEY)."
        }
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = key
        )
    }
}
