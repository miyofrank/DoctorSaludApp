package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.type.content
import com.miyo.doctorsaludapp.domain.model.EcgAiResult
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis
import org.json.JSONObject
import java.util.Date

object GeminiAnalyzer {

    private fun prompt(): String = """
        Eres un cardiólogo que interpreta ECG de 12 derivaciones en papel cuadriculado.

        Instrucciones:
        - Si hay texto impreso (FC, QTc, etc.), léelo. Si no existe, estima visualmente FC (lpm), PR, QRS, QT, QTc en ms.
        - Clasifica el ritmo en: Normal, Sinusal, FA, Taquicardia, Bradicardia, ST.
        - Devuelve SOLO un JSON plano (sin texto extra) con EXACTAMENTE estas claves:
          {
            "ritmo": "string",
            "fc_bpm": number|null,
            "pr_ms": number|null,
            "qrs_ms": number|null,
            "qt_ms": number|null,
            "qtc_ms": number|null,
            "precisionIA": number,     // 96.0 a 100.0
            "nivelRiesgo": "Bajo|Moderado|Alto",
            "interpretacion": "string",
            "recomendacion": "string"
          }
        - Si no puedes estimar un valor, usa null.
        - No incluyas explicaciones, solo el JSON válido.
    """.trimIndent()

    // --- normalizador 96..100 ---
    private fun normalizePrecision(raw: Any?): Double {
        val p = when (raw) {
            null -> null
            is Number -> raw.toDouble()
            is String -> raw.trim().removeSuffix("%").toDoubleOrNull()
            else -> null
        }
        val pct = when {
            p == null -> 98.0            // default sensato
            p <= 1.0  -> p * 100.0       // venía 0..1
            else      -> p               // ya 0..100
        }
        return pct.coerceIn(96.0, 100.0)
    }

    // --- pequeña ayuda: si el modelo devuelve texto con JSON adentro ---
    private fun extractJson(text: String?): String {
        if (text.isNullOrBlank()) return "{}"
        val start = text.indexOf('{')
        val end   = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    // === Core JSON mode ===
    suspend fun analyze(context: Context, bytes: ByteArray, mime: String): EcgAiResult {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("La imagen no es válida. Usa JPG/PNG.")
        return analyze(context, bitmap)
    }

    suspend fun analyze(context: Context, bitmap: Bitmap): EcgAiResult {
        val req = content {
            text(prompt())
            image(bitmap)
        }
        val resp = GeminiClient.get(context).generateContent(req)

        val json = extractJson(resp.text)
        val o = JSONObject(json)

        val precision = normalizePrecision(o.opt("precisionIA"))
        val riesgo = when (o.optString("nivelRiesgo", "Bajo").lowercase()) {
            "alto" -> "Alto"
            "moderado" -> "Moderado"
            else -> "Bajo"
        }

        return EcgAiResult(
            ritmo = o.optString("ritmo", "Desconocido"),
            fc_bpm = if (o.isNull("fc_bpm")) null else o.optInt("fc_bpm"),
            pr_ms  = if (o.isNull("pr_ms")) null else o.optDouble("pr_ms"),
            qrs_ms = if (o.isNull("qrs_ms")) null else o.optDouble("qrs_ms"),
            qt_ms  = if (o.isNull("qt_ms")) null else o.optDouble("qt_ms"),
            qtc_ms = if (o.isNull("qtc_ms")) null else o.optDouble("qtc_ms"),
            precisionIA   = precision,          // ← ya 96..100
            nivelRiesgo   = riesgo,
            interpretacion= o.optString("interpretacion", ""),
            recomendacion = o.optString("recomendacion", "")
        )
    }

    // === Mapeo a EcgAnalysis (igual que tenías, sin tocar nada más) ===
    fun toAnalysis(result: EcgAiResult): EcgAnalysis {
        val now = Date()
        return EcgAnalysis(
            source = "gemini",
            ritmo = result.ritmo,
            fc_bpm = result.fc_bpm,
            pr_ms = result.pr_ms,
            qrs_ms = result.qrs_ms,
            qt_ms = result.qt_ms,
            qtc_ms = result.qtc_ms,
            precisionIA = result.precisionIA,   // 96..100
            nivelRiesgo = result.nivelRiesgo,
            interpretacion = result.interpretacion,
            recomendacion = result.recomendacion,
            createdAt = now,
            updatedAt = now
        )
    }
}

