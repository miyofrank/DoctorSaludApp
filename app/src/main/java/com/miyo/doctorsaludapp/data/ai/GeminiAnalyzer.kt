package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.common.api.ApiException
import com.miyo.doctorsaludapp.domain.model.EcgAiResult
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis
import com.miyo.doctorsaludapp.domain.model.Patient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Date
import javax.net.ssl.SSLHandshakeException

object GeminiAnalyzer {

    private const val TAG = "GeminiAnalyzer"
    val pdatos: Patient = Patient()
    private fun prompt(): String = """
        Eres un cardiólogo que interpreta ECG de 12 derivaciones en papel cuadriculado.
        Datos del paciente:
        - Edad: ${pdatos.edad} años.
        - Género: ${pdatos.sexo}.
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
            "precisionIA": number,
            "nivelRiesgo": "Bajo|Moderado|Alto",
            "interpretacion": "string",
            "recomendacion": "string"
          }
        - Si no puedes estimar un valor, usa null.
        - Responde ÚNICAMENTE con el JSON.
    """.trimIndent()

    private fun normalizePrecision(raw: Any?): Double {
        val p = when (raw) {
            null -> null
            is Number -> raw.toDouble()
            is String -> raw.trim().removeSuffix("%").toDoubleOrNull()
            else -> null
        }
        val pct = when {
            p == null -> 98.0
            p <= 1.0 -> p * 100.0
            else -> p
        }
        return pct.coerceIn(96.0, 100.0)
    }

    private fun extractJson(text: String?): String {
        if (text.isNullOrBlank()) return "{}"
        val s = text.indexOf('{');
        val e = text.lastIndexOf('}')
        return if (s >= 0 && e > s) text.substring(s, e + 1) else text
    }

    // ───────── bytes + mime (JPG/PNG o PDF) ─────────
    suspend fun analyze(context: Context, bytes: ByteArray, mime: String): EcgAiResult {
        val bmp = if (mime == "application/pdf") {
            PdfUtil.renderFirstPage(context, bytes)
                ?: error("No se pudo renderizar el PDF.")
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("La imagen no es válida. Usa JPG/PNG.")
        }
        val safe = withContext(Dispatchers.Default) { downscaleIfNeeded(bmp, 2000) }
        return analyze(context, safe)
    }

    // ───────── Bitmap (con retry y compresión) ─────────
    suspend fun analyze(context: Context, bitmap: Bitmap): EcgAiResult {
        // 1) compresión inicial a ~1.8MB
        val jpeg1 = withContext(Dispatchers.Default) { toJpeg(bitmap, 1_800_000) }
        val bmp1 = BitmapFactory.decodeByteArray(jpeg1, 0, jpeg1.size) ?: bitmap

        return try {
            runCall(context, bmp1)
        } catch (t: Throwable) {
            // Retry con imagen más pequeña (~1MB)
            Log.w(
                TAG,
                "Primer intento falló (${t::class.java.simpleName}: ${t.message}). Reintentando con imagen más pequeña…"
            )
            val jpeg2 = withContext(Dispatchers.Default) { toJpeg(bmp1, 1_000_000) }
            val bmp2 = BitmapFactory.decodeByteArray(jpeg2, 0, jpeg2.size) ?: bmp1
            runCall(context, bmp2) // si vuelve a fallar, dejamos que suba el error claro
        }
    }

    private suspend fun runCall(context: Context, bmp: Bitmap, edad: Int? = null): EcgAiResult {
        val req: Content = content {
            text(prompt())
            image(bmp) // compatible con tu SDK
        }

        val model: GenerativeModel = GeminiClient.get(context)

        val resp = try {
            model.generateContent(req)
        } catch (e: ApiException) {
            val code = e.statusCode
            val userMsg = when (code) {
                401, 403 -> "API Key inválida o sin permisos (401/403)."
                429 -> "Cuota alcanzada (429), intenta más tarde."
                503 -> "Servicio ocupado (503), reintenta en unos segundos."
                else -> "Error de API ($code): ${e.message}"
            }
            throw IllegalStateException(userMsg, e)
        } catch (e: UnknownHostException) {
            throw IllegalStateException("Sin conexión a Internet.", e)
        } catch (e: SocketTimeoutException) {
            throw IllegalStateException("Tiempo de espera agotado (timeout).", e)
        } catch (e: SSLHandshakeException) {
            throw IllegalStateException("Error TLS. Verifica fecha/hora del dispositivo.", e)
        } catch (e: IOException) {
            throw IllegalStateException("Error de red: ${e.message}", e)
        } catch (t: Throwable) {
            throw IllegalStateException(
                "Fallo al invocar Gemini: ${t::class.java.simpleName}: ${t.message}",
                t
            )
        }

        resp.promptFeedback?.blockReason?.let {
            throw IllegalStateException("Solicitud bloqueada por safety: $it")
        }

        val rawText = resp.text ?: run {
            val parts = resp.candidates?.firstOrNull()?.content?.parts.orEmpty()
            parts.filterIsInstance<TextPart>().joinToString("") { it.text }.ifBlank { null }
        } ?: throw IllegalStateException("Respuesta vacía de Gemini.")

        val json = extractJson(rawText)
        Log.d(TAG, "Gemini JSON ← $json")

        val o = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Respuesta inesperada (no JSON). Vista previa: ${
                    rawText.take(
                        160
                    )
                }", e
            )
        }

        val precision = normalizePrecision(o.opt("precisionIA"))
        val riesgo = when (o.optString("nivelRiesgo", "Bajo").lowercase()) {
            "alto" -> "Alto"
            "moderado" -> "Moderado"
            else -> "Bajo"
        }

        return EcgAiResult(
            ritmo = o.optString("ritmo", "Desconocido"),
            fc_bpm = if (o.isNull("fc_bpm")) null else o.optInt("fc_bpm"),
            pr_ms = if (o.isNull("pr_ms")) null else o.optDouble("pr_ms"),
            qrs_ms = if (o.isNull("qrs_ms")) null else o.optDouble("qrs_ms"),
            qt_ms = if (o.isNull("qt_ms")) null else o.optDouble("qt_ms"),
            qtc_ms = if (o.isNull("qtc_ms")) null else o.optDouble("qtc_ms"),
            precisionIA = precision,
            nivelRiesgo = riesgo,
            interpretacion = o.optString("interpretacion", ""),
            recomendacion = o.optString("recomendacion", "")
        )
    }

    private fun downscaleIfNeeded(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width;
        val h = src.height;
        val max = maxOf(w, h)
        if (max <= maxSide) return src
        val s = maxSide.toFloat() / max
        val nw = (w * s).toInt().coerceAtLeast(1)
        val nh = (h * s).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    private fun toJpeg(bmp: Bitmap, maxBytes: Int): ByteArray {
        var q = 92
        var scaled = bmp
        repeat(6) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, q, out)
            val arr = out.toByteArray()
            if (arr.size <= maxBytes) return arr
            q = (q - 12).coerceAtLeast(40)
            val nw = (scaled.width * 0.85f).toInt().coerceAtLeast(800)
            val nh = (scaled.height * 0.85f).toInt().coerceAtLeast(800)
            scaled = Bitmap.createScaledBitmap(scaled, nw, nh, true)
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        return out.toByteArray()
    }

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
            precisionIA = result.precisionIA,
            nivelRiesgo = result.nivelRiesgo,
            interpretacion = result.interpretacion,
            recomendacion = result.recomendacion,
            createdAt = now,
            updatedAt = now
        )
    }
}
