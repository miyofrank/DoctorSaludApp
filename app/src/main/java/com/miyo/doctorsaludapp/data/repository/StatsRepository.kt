package com.miyo.doctorsaludapp.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.domain.model.stats.MonthTrend
import com.miyo.doctorsaludapp.domain.model.stats.RiskDistribution
import com.miyo.doctorsaludapp.domain.model.stats.StatsSummary
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import kotlin.math.max

/**
 * Lee estadísticas desde pacientes/{pid}/ecgs/{ecgId}
 * usando SIEMPRE analysis.* (según tu screenshot real).
 */
class StatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val patientsCollection: String = "pacientes"
) {
    private val TAG = "StatsRepository"

    // ===== Helpers de parseo seguros =====
    private fun Any?.asDate(): Date? = when (this) {
        is Timestamp -> this.toDate()
        is Date      -> this
        is Long      -> Date(this)
        is String    -> null // no parseamos fechas en string aquí
        else         -> null
    }
    private fun Any?.asDouble(): Double? = when (this) {
        is Number -> this.toDouble()
        is String -> this.trim().removeSuffix("%").toDoubleOrNull()
        else      -> null
    }
    private fun Any?.asString(): String? = this as? String

    private data class Parsed(
        val createdAt: Date?,     // analysis.createdAt si existe; si no, root.createdAt/updatedAt
        val analyzedAt: Date?,    // analysis.analyzedAt si existe; si no, root.analyzedAt/updatedAt
        val precisionPct: Double?,// normalizado a 0..100
        val riesgo: String?       // bajo | moderado | alto | critico/crítico
    )

    suspend fun fetchStats(lastMonths: Int = 12): StatsSummary {
        // 1) Pacientes totales (conteo simple de colección pacientes)
        val totalPacientes = try {
            db.collection(patientsCollection).get().await().size()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo contar pacientes: ${e.message}")
            0
        }

        // 2) Rango de fecha para createdAt
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.MONTH, -max(1, lastMonths))
        val start = cal.time

        // 3) Traer ECGs: primero con rango+orderBy; si vacío o falla índice, fallback sin rango
        val docs = try {
            val withRange = db.collectionGroup("ecgs")
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
            if (withRange.isNotEmpty()) {
                Log.i(TAG, "ECGs con rango: ${withRange.size}")
                withRange
            } else {
                Log.i(TAG, "Sin ECGs por rango. Fallback sin rango/orden…")
                db.collectionGroup("ecgs")
                    .limit(1000)
                    .get()
                    .await()
                    .documents
                    .also { Log.i(TAG, "ECGs fallback: ${it.size}") }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Query con rango falló: ${e.message}. Fallback sin rango…")
            try {
                db.collectionGroup("ecgs").limit(1000).get().await().documents
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback también falló: ${e2.message}")
                emptyList()
            }
        }

        // 4) Parsear usando analysis.*
        val items = docs.mapNotNull { d ->
            val root = d.data ?: return@mapNotNull null
            val analysis = root["analysis"] as? Map<*, *> ?: return@mapNotNull null

            // Fechas preferentemente dentro de analysis (según tu doc)
            val createdAtAna = analysis["createdAt"].asDate()
            val analyzedAtAna = analysis["analyzedAt"].asDate()

            // Fallbacks por si algún doc viejo los dejó en raíz
            val createdAtRoot = root["createdAt"].asDate() ?: root["updatedAt"].asDate()
            val analyzedAtRoot = root["analyzedAt"].asDate() ?: root["updatedAt"].asDate()

            val created = createdAtAna ?: createdAtRoot
            val analyzed = analyzedAtAna ?: analyzedAtRoot

            // precisionIA puede venir como 0..1, 0..100 o "94.2%"
            val precisionRaw = analysis["precisionIA"].asDouble()
            val precisionPct = precisionRaw?.let { if (it <= 1.0) it * 100.0 else it }

            // riesgo
            val riesgo = analysis["nivelRiesgo"].asString()?.lowercase()

            Parsed(
                createdAt = created,
                analyzedAt = analyzed,
                precisionPct = precisionPct,
                riesgo = riesgo
            )
        }

        Log.i(TAG, "ECGs parseados (analysis.*): ${items.size}")

        // 5) Métricas globales
        val avgPrecision = items.mapNotNull { it.precisionPct }
            .takeIf { it.isNotEmpty() }?.average()

        val iaSeconds = items.mapNotNull { e ->
            val c = e.createdAt?.time ?: return@mapNotNull null
            val a = e.analyzedAt?.time ?: return@mapNotNull null
            (a - c).coerceAtLeast(0) / 1000.0
        }
        val avgIaSeconds = iaSeconds.takeIf { it.isNotEmpty() }?.average()
        val savedMinutes = avgIaSeconds?.let { 15.5 - (it / 60.0) }

        // 6) Distribución de riesgo
        val dist = RiskDistribution(
            bajo = items.count { it.riesgo == "bajo" },
            moderado = items.count { it.riesgo == "moderado" },
            alto = items.count { it.riesgo == "alto" },
            critico = items.count { it.riesgo == "crítico" || it.riesgo == "critico" }
        )

        // 7) Tendencias por mes (solo si tenemos createdAt)
        val months = mutableMapOf<Pair<Int,Int>, MutableList<Parsed>>()
        val tmp = Calendar.getInstance()
        items.forEach { e ->
            val dt = e.createdAt ?: return@forEach
            tmp.time = dt
            val key = tmp.get(Calendar.YEAR) to (tmp.get(Calendar.MONTH) + 1)
            months.getOrPut(key) { mutableListOf() }.add(e)
        }

        val monthly = months.toList()
            .sortedWith(compareBy({ it.first.first }, { it.first.second })) // (year, month)
            .map { (ym, list) ->
                val (y, m) = ym
                val avgS = list.mapNotNull { d ->
                    val c = d.createdAt?.time ?: return@mapNotNull null
                    val a = d.analyzedAt?.time ?: return@mapNotNull null
                    (a - c).coerceAtLeast(0) / 1000.0
                }.takeIf { it.isNotEmpty() }?.average()
                val avgP = list.mapNotNull { it.precisionPct }.takeIf { it.isNotEmpty() }?.average()
                MonthTrend(
                    year = y,
                    month = m,
                    count = list.size,
                    avgIaSeconds = avgS,
                    avgPrecision = avgP
                )
            }

        return StatsSummary(
            totalPacientes = totalPacientes,
            avgPrecisionGlobal = avgPrecision,
            avgIaSeconds = avgIaSeconds,
            savedMinutes = savedMinutes,
            risk = dist,
            monthly = monthly
        )
    }
}

