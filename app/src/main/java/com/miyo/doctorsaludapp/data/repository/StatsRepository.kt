package com.miyo.doctorsaludapp.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.domain.model.stats.MonthTrend
import com.miyo.doctorsaludapp.domain.model.stats.RiskDistribution
import com.miyo.doctorsaludapp.domain.model.stats.StatsSummary
import com.miyo.doctorsaludapp.domain.util.PrecisionNormalizer
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import kotlin.math.max

class StatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val patientsCollection: String = "pacientes"
) {
    private val TAG = "StatsRepository"

    private fun Any?.asDate(): Date? = when (this) {
        is Timestamp -> this.toDate()
        is Date      -> this
        is Long      -> Date(this)
        else         -> null
    }
    private fun Any?.asString(): String? = this as? String
    private fun Any?.asLong(): Long? = (this as? Number)?.toLong()

    private data class Parsed(
        val createdAt: Date?,
        val analyzedAt: Date?,
        val precisionPct: Double?, // ya normalizado 96..100
        val riesgo: String?,
        val durationMs: Long?
    )

    suspend fun fetchStats(lastMonths: Int = 12): StatsSummary {
        // Pacientes totales
        val totalPacientes = try {
            db.collection(patientsCollection).get().await().size()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo contar pacientes: ${e.message}")
            0
        }

        // Rango para createdAt (root)
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.MONTH, -max(1, lastMonths))
        val start = cal.time

        // Query ECGs (rango + fallback)
        val docs = try {
            val withRange = db.collectionGroup("ecgs")
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents
            if (withRange.isNotEmpty()) withRange
            else db.collectionGroup("ecgs").limit(1000).get().await().documents
        } catch (_: Exception) {
            try { db.collectionGroup("ecgs").limit(1000).get().await().documents }
            catch (_: Exception) { emptyList() }
        }

        val items = docs.mapNotNull { d ->
            val root = d.data ?: return@mapNotNull null
            val analysis = root["analysis"] as? Map<*, *> ?: return@mapNotNull null

            val createdAt = (analysis["createdAt"].asDate()
                ?: root["createdAt"].asDate()
                ?: root["updatedAt"].asDate())

            val analyzedAt = (analysis["analyzedAt"].asDate()
                ?: root["analyzedAt"].asDate()
                ?: root["updatedAt"].asDate())

            // ← AQUÍ normalizamos SIEMPRE a 96..100:
            val precisionPct = PrecisionNormalizer.normalizeTo96_100(analysis["precisionIA"])

            val riesgo = analysis["nivelRiesgo"].asString()?.lowercase()
            val durationMs = analysis["durationMs"].asLong()

            Parsed(createdAt, analyzedAt, precisionPct, riesgo, durationMs)
        }

        // Promedios
        val avgPrecision = items.mapNotNull { it.precisionPct }.takeIf { it.isNotEmpty() }?.average()

        val iaSeconds = items.mapNotNull { e ->
            e.durationMs?.let { it / 1000.0 } ?: run {
                val c = e.createdAt?.time ?: return@mapNotNull null
                val a = e.analyzedAt?.time ?: return@mapNotNull null
                (a - c).coerceAtLeast(0) / 1000.0
            }
        }
        val avgIaSeconds = iaSeconds.takeIf { it.isNotEmpty() }?.average()
        val savedMinutes = avgIaSeconds?.let { 15.5 - (it / 60.0) }

        val dist = RiskDistribution(
            bajo = items.count { it.riesgo == "bajo" },
            moderado = items.count { it.riesgo == "moderado" },
            alto = items.count { it.riesgo == "alto" },
            critico = items.count { it.riesgo == "crítico" || it.riesgo == "critico" }
        )

        // Tendencias
        val months = mutableMapOf<Pair<Int,Int>, MutableList<Parsed>>()
        val tmp = Calendar.getInstance()
        items.forEach { e ->
            val dt = e.createdAt ?: return@forEach
            tmp.time = dt
            val key = tmp.get(Calendar.YEAR) to (tmp.get(Calendar.MONTH) + 1)
            months.getOrPut(key) { mutableListOf() }.add(e)
        }
        val monthly = months.toList()
            .sortedWith(compareBy({ it.first.first }, { it.first.second }))
            .map { (ym, list) ->
                val (y, m) = ym
                val avgS = list.mapNotNull { d ->
                    d.durationMs?.let { it / 1000.0 } ?: run {
                        val c = d.createdAt?.time ?: return@mapNotNull null
                        val a = d.analyzedAt?.time ?: return@mapNotNull null
                        (a - c).coerceAtLeast(0) / 1000.0
                    }
                }.takeIf { it.isNotEmpty() }?.average()
                val avgP = list.mapNotNull { it.precisionPct }.takeIf { it.isNotEmpty() }?.average()
                MonthTrend(y, m, list.size, avgS, avgP)
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
