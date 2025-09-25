package com.miyo.doctorsaludapp.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.DocumentSnapshot
import com.miyo.doctorsaludapp.domain.model.stats.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Excepción que indica que falta crear un índice para una query de collection group.
 * Lleva un resultado parcial para que la UI pueda mostrar algo mientras tanto.
 */
class CollectionGroupIndexRequiredException(
    val partial: StatsResult,
    message: String
) : IllegalStateException(message)

/**
 * KPIs:
 *  - Total pacientes: colección raíz "pacientes"
 *  - Métricas: collection group "ecgs"
 *    Campos tolerados (plano o bajo "analysis"):
 *      fechas: updatedAt | analyzedAt | createdAt | analysis.updatedAt | analysis.analyzedAt
 *      precisión: precisionIA | precision | analysis.precisionIA
 *      duración: durationMs | durationMS | durationMillis | tiempoIaMs | tiempoIAms |
 *                durationSec | durationSecs | durationSeconds | analysis.durationMs/MS/…
 *      riesgo: nivelRiesgo | riesgo | analysis.nivelRiesgo
 */
class StatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val pacientesCollection: String = "pacientes",
    private val ecgCollectionGroup: String = "ecgs"
) {

    suspend fun fetch(filters: StatsFilters): StatsResult {
        // 1) Total pacientes
        val collPac = db.collection(pacientesCollection)
        val totalPacientes: Int = try {
            collPac.count().get(AggregateSource.SERVER).await().count.toInt()
        } catch (_: Throwable) {
            collPac.limit(1000).get().await().size()
        }

        val startDate: Date = filters.start
        val endDate: Date = filters.end

        // 2) Queries (por si algunos usan updatedAt y otros analyzedAt)
        val snapUpdated = try {
            db.collectionGroup(ecgCollectionGroup)
                .whereGreaterThanOrEqualTo("updatedAt", startDate)
                .whereLessThanOrEqualTo("updatedAt", endDate)
                .get()
                .await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                val partial = StatsResult(totalPacientes = totalPacientes)
                throw CollectionGroupIndexRequiredException(
                    partial = partial,
                    message = "Falta crear el índice (collection group=$ecgCollectionGroup, campo=updatedAt)."
                )
            } else throw e
        }

        val snapAnalyzed = try {
            db.collectionGroup(ecgCollectionGroup)
                .whereGreaterThanOrEqualTo("analyzedAt", startDate)
                .whereLessThanOrEqualTo("analyzedAt", endDate)
                .get()
                .await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                Log.w("StatsRepo", "Falta índice *de campo único* para analyzedAt en collection group $ecgCollectionGroup")
                null
            } else throw e
        }

        // Merge por path
        val docsMap = LinkedHashMap<String, DocumentSnapshot>()
        for (d in snapUpdated.documents) docsMap[d.reference.path] = d
        if (snapAnalyzed != null) for (d in snapAnalyzed.documents) docsMap[d.reference.path] = d

        Log.d("StatsRepo",
            "ECG docs: updatedAt=${snapUpdated.size()} analyzedAt=${snapAnalyzed?.size() ?: 0} merged=${docsMap.size}"
        )

        // 3) Agregación
        var sumPrecision = 0.0; var nPrecision = 0
        var sumIaSec = 0.0; var nIa = 0
        var riskB = 0; var riskM = 0; var riskA = 0; var riskC = 0

        data class Agg(var count:Int=0, var sumP:Double=0.0, var nP:Int=0, var sumIa:Double=0.0, var nIa:Int=0)
        val byKey = HashMap<String, Agg>()

        val gran = filters.granularity.lowercase(Locale.getDefault()) // day|month|year
        val dfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val dfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        fun keyOf(d: Date) = when (gran) { "day" -> dfDay.format(d); "year" -> dfYear.format(d); else -> dfMonth.format(d) }

        for ((_, doc) in docsMap) {
            val analysis = (doc.get("analysis") as? Map<*, *>) ?: emptyMap<Any, Any>()

            // Fecha efectiva (prioridad updatedAt > analyzedAt > createdAt; plano o analysis.*)
            val updatedAny = doc.get("updatedAt")
                ?: analysis["updatedAt"]
                ?: doc.get("analyzedAt")
                ?: analysis["analyzedAt"]
                ?: doc.get("createdAt")
            val updatedDate: Date = anyToDate(updatedAny) ?: continue

            // Precisión (plano o analysis)
            val precision = parsePrecision(
                doc.get("precisionIA")
                    ?: doc.get("precision")
                    ?: analysis["precisionIA"]
            )
            if (precision != null) { sumPrecision += precision; nPrecision++ }

            // Tiempo IA (plano o analysis), ms o s, con fallback (end-start)
            val iaSeconds = readIaSeconds(doc, analysis, updatedDate)
            if (iaSeconds != null) { sumIaSec += iaSeconds; nIa++ }

            // Riesgo (plano o analysis, normaliza)
            val riskStr = (doc.get("nivelRiesgo") ?: doc.get("riesgo") ?: analysis["nivelRiesgo"])
                ?.toString()?.lowercase(Locale.getDefault())
            when (riskStr) {
                "bajo", "low" -> riskB++
                "moderado", "moderate", "medium" -> riskM++
                "alto", "high" -> riskA++
                "critico", "crítico", "critical", "cr\u00edtico" -> riskC++
            }

            // Agregación por período
            val k = keyOf(updatedDate)
            val a = byKey.getOrPut(k) { Agg() }
            a.count++
            if (precision != null) { a.sumP += precision; a.nP++ }
            if (iaSeconds != null) { a.sumIa += iaSeconds; a.nIa++ }
        }

        val points = byKey.toList().sortedBy { it.first }.map { (k, a) ->
            val (y, m, d) = splitKey(k, gran)
            MonthlyPoint(
                year = y, month = m, day = d,
                monthLabel = labelFor(k, gran, y, m),
                count = a.count,
                avgPrecision = if (a.nP > 0) a.sumP / a.nP else null,
                avgIaSeconds = if (a.nIa > 0) a.sumIa / a.nIa else null
            )
        }

        val avgP = if (nPrecision > 0) sumPrecision / nPrecision else null
        val avgIa = if (nIa > 0) sumIaSec / nIa else null
        val avgManual = 15.5
        val saved = avgIa?.let { max(0.0, avgManual - (it / 60.0)) }

        return StatsResult(
            totalPacientes = totalPacientes,
            avgPrecisionGlobal = avgP,
            avgIaSeconds = avgIa,
            avgManualMinutes = avgManual,
            savedMinutes = saved,
            risk = RiskBucket(bajo = riskB, moderado = riskM, alto = riskA, critico = riskC),
            monthly = points
        )
    }

    // ---------- Helpers ----------

    private fun anyToDate(any: Any?): Date? = when (any) {
        is Timestamp -> any.toDate()
        is Date -> any
        is Long -> Date(any)
        else -> null
    }

    private fun parsePrecision(any: Any?): Double? {
        val p = when (any) {
            null -> return null
            is Number -> any.toDouble()
            is String -> any.trim().removeSuffix("%").toDoubleOrNull() ?: return null
            else -> return null
        }
        val pct = if (p in 0.0..1.0) p * 100.0 else p
        return pct.coerceIn(0.0, 100.0)
    }

    /**
     * Segundos de IA aceptando claves en raíz o analysis:
     *  - ms: durationMs/MS/Millis/tiempoIaMs/tiempoIAms
     *  - s : durationSec/Secs/Seconds
     *  Fallback: (end - start) si hay timestamps.
     */
    private fun readIaSeconds(
        doc: DocumentSnapshot,
        analysis: Map<*, *>,
        endDate: Date
    ): Double? {
        fun numOf(map: Map<*, *>, vararg keys: String): Double? {
            for (k in keys) {
                val v = map[k]
                (v as? Number)?.toDouble()?.let { return it }
                (v as? String)?.trim()?.replace(",", ".")?.toDoubleOrNull()?.let { return it }
            }
            return null
        }
        fun numOfDoc(vararg keys: String): Double? {
            for (k in keys) {
                val v = doc.get(k)
                (v as? Number)?.toDouble()?.let { return it }
                (v as? String)?.trim()?.replace(",", ".")?.toDoubleOrNull()?.let { return it }
            }
            return null
        }

        // ms en raíz o analysis
        val msRoot = numOfDoc("durationMs", "durationMS", "durationMillis", "tiempoIaMs", "tiempoIAms")
        val msAnalysis = numOf(analysis, "durationMs", "durationMS", "durationMillis", "tiempoIaMs", "tiempoIAms")
        val ms = msRoot ?: msAnalysis
        if (ms != null) return ms / 1000.0

        // s en raíz o analysis
        val sRoot = numOfDoc("durationSec", "durationSecs", "durationSeconds")
        val sAnalysis = numOf(analysis, "durationSec", "durationSecs", "durationSeconds")
        val seconds = sRoot ?: sAnalysis
        if (seconds != null) return seconds

        // fallback: end - start (start puede venir de createdAt/startedAt/analysis.startedAt)
        val startAny = doc.get("createdAt") ?: doc.get("startedAt") ?: analysis["startedAt"]
        val start = anyToDate(startAny)
        if (start != null && endDate.time > start.time) {
            return (endDate.time - start.time) / 1000.0
        }

        return null
    }

    private fun splitKey(key: String, gran: String): Triple<Int, Int, Int?> {
        return when (gran) {
            "year" -> Triple(key.toIntOrNull() ?: 1970, 1, null)
            "day" -> {
                val parts = key.split("-")
                Triple(parts.getOrNull(0)?.toIntOrNull() ?: 1970, parts.getOrNull(1)?.toIntOrNull() ?: 1, parts.getOrNull(2)?.toIntOrNull())
            }
            else -> { // month
                val parts = key.split("-")
                Triple(parts.getOrNull(0)?.toIntOrNull() ?: 1970, parts.getOrNull(1)?.toIntOrNull() ?: 1, null)
            }
        }
    }

    private fun labelFor(key: String, gran: String, year: Int, month: Int): String {
        return when (gran) {
            "year" -> key
            "day" -> key
            else -> monthLabel(year, month)
        }
    }

    private fun monthLabel(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return fmt.format(cal.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
