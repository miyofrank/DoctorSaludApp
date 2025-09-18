package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.type.content
import com.miyo.doctorsaludapp.domain.model.EcgAiResult
import org.json.JSONObject

/**
 * Analiza un ECG (imagen) con Gemini y devuelve EcgAiResult.
 * JSON mode ya está configurado en el modelo (GeminiClient).
 */
object GeminiAnalyzer {

    private fun prompt(): String = """
    Eres un cardiólogo que interpreta ECG de 12 derivaciones en papel cuadriculado.
    Instrucciones:
    - Si hay texto impreso (FC, QTc, etc.), léelo. Si no existe, estima visualmente FC (lpm), PR, QRS, QT, QTc en ms.
    - Clasifica el ritmo en: Normal, Sinusal, FA, Taquicardia, Bradicardia, ST.
    - Devuelve SOLO un JSON con exactamente estos campos:
      {
        "ritmo": "string",
        "fc_bpm": number|null,
        "pr_ms": number|null,
        "qrs_ms": number|null,
        "qt_ms": number|null,
        "qtc_ms": number|null,
        "precisionIA": number|null,
        "nivelRiesgo": "Bajo|Moderado|Alto",
        "interpretacion": "string",
        "recomendacion": "string"
      }
    - Si no puedes estimar un valor, usa null. No agregues nada fuera del JSON.
  """.trimIndent()

    suspend fun analyze(context: Context, bytes: ByteArray, mime: String): EcgAiResult {
        // Convierte a Bitmap para el DSL (tu SDK no tiene inlineData)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("La imagen no es válida. Usa JPG/PNG.")

        // Construye el contenido con el DSL
        val req = content {
            text(prompt())
            image(bitmap)
        }

        // Llama al modelo (el JSON mode ya está puesto en GeminiClient)
        val resp = GeminiClient.get(context).generateContent(req)

        // Parseo del JSON
        val json = resp.text ?: "{}"
        val o = JSONObject(json)
        return EcgAiResult(
            ritmo = o.optString("ritmo", "Desconocido"),
            fc_bpm = if (o.isNull("fc_bpm")) null else o.optInt("fc_bpm"),
            pr_ms  = if (o.isNull("pr_ms")) null else o.optDouble("pr_ms"),
            qrs_ms = if (o.isNull("qrs_ms")) null else o.optDouble("qrs_ms"),
            qt_ms  = if (o.isNull("qt_ms")) null else o.optDouble("qt_ms"),
            qtc_ms = if (o.isNull("qtc_ms")) null else o.optDouble("qtc_ms"),
            precisionIA   = if (o.isNull("precisionIA")) null else o.optDouble("precisionIA"),
            nivelRiesgo   = o.optString("nivelRiesgo", "Bajo"),
            interpretacion= o.optString("interpretacion", ""),
            recomendacion = o.optString("recomendacion", "")
        )
    }
}

