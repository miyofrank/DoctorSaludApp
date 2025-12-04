package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

object GeminiHealth {
    private const val TAG = "GeminiHealth"

    suspend fun check(context: Context): String = withContext(Dispatchers.IO) {
        val model = GeminiClient.get(context)
        return@withContext try {
            val resp = model.generateContent("ping")
            val txt = resp.text ?: "(sin texto)"
            Log.i(TAG, "Ping OK: $txt")
            "OK"
        } catch (e: ApiException) {
            "API error ${e.statusCode}: ${e.message}"
        } catch (e: UnknownHostException) {
            "Sin conexión a Internet."
        } catch (t: Throwable) {
            "Fallo desconocido: ${t::class.java.simpleName}: ${t.message}"
        }
    }
}
