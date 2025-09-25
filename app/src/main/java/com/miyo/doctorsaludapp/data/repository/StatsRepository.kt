package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.domain.stats.Granularity
import com.miyo.doctorsaludapp.domain.stats.StatsFilters
import kotlinx.coroutines.tasks.await
import java.util.*

data class RiskCounts(val bajo:Int=0, val moderado:Int=0, val alto:Int=0, val critico:Int=0)

data class MonthlyPoint(
    val monthLabel: String,
    val count: Int,
    val avgPrecision: Double?,   // 0..100
    val avgIaSeconds: Double?    // segundos
)

data class StatsResult(
    val totalPacientes: Int,
    val avgPrecisionGlobal: Double?, // 96..100 (normalizado fuera)
    val avgIaSeconds: Double?,       // segundos
    val avgManualMinutes: Double = 15.5,
    val savedMinutes: Double?,       // minutos
    val risk: RiskCounts,
    val monthly: List<MonthlyPoint>
)

class StatsRepository(
    private val db: FirebaseFirestore,
    private val collection: String
) {
    private fun asDate(any: Any?): Date? = when (any) {
        is Timestamp -> any.toDate()
        is Date -> any
        is Number -> Date(any.toLong())
        else -> null
    }

    private fun normPrecision(v: Any?): Double? {
        val p = when (v) {
            is Number -> v.toDouble()
            is String -> v.trim().removeSuffix("%").toDoubleOrNull()
            else -> null
        } ?: return null
        val scaled = if (p <= 1.0) p * 100.0 else p
        return scaled.coerceIn(0.0, 100.0)
    }

    suspend fun fetchStats(filters: StatsFilters): StatsResult {
        // Consulta base (orden por createdAt para paginar mejor)
        val snaps = db.collection(collection)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val rows = mutableListOf<Triple<Double, String, Double>>() // (precision, riesgo, iaSeconds)
        val bucketMap = LinkedHashMap<String, MutableList<Triple<Double, String, Double>>>()

        fun labelFor(date: Date): String {
            val c = Calendar.getInstance().apply { time = date }
            return when (filters.granularity) {
                Granularity.DAY ->
                    "%02d/%02d".format(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH)+1)
                Granularity.MONTH ->
                    arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[c.get(Calendar.MONTH)]
                Granularity.YEAR ->
                    c.get(Calendar.YEAR).toString()
            }
        }

        snaps.documents.forEach { d ->
            val analysis = (d.get("analysis") as? Map<*, *>) ?: return@forEach
            val whenDt  = asDate(analysis["updatedAt"]) ?: asDate(analysis["createdAt"])
            ?: d.getDate("updatedAt") ?: d.getDate("createdAt") ?: Date(0)

            // filtrar por rango
            if (whenDt.before(filters.start) || whenDt.after(filters.end)) return@forEach

            val precision = (normPrecision(analysis["precisionIA"]) ?: 98.0)
            val riesgo = (analysis["nivelRiesgo"] as? String)?.lowercase() ?: "bajo"
            val iaSec = (analysis["iaSeconds"] as? Number)?.toDouble() ?: 3.0

            rows += Triple(precision, riesgo, iaSec)

            val key = labelFor(whenDt)
            val list = bucketMap.getOrPut(key) { mutableListOf() }
            list += Triple(precision, riesgo, iaSec)
        }

        val totalPac = snaps.size()

        val avgPrecision = rows.map { it.first }.let { if (it.isNotEmpty()) it.average() else null }
        val avgIaSeconds = rows.map { it.third }.let { if (it.isNotEmpty()) it.average() else null }
        val savedMinutes = avgIaSeconds?.let { // IA en segundos → minutos ahorrados vs 15.5 min manual
            val iaMin = it / 60.0
            (15.5 - iaMin).coerceAtLeast(0.0)
        }

        val riskCounts = rows.groupingBy { it.second }.eachCount()
        val risk = RiskCounts(
            bajo = riskCounts["bajo"] ?: 0,
            moderado = riskCounts["moderado"] ?: 0,
            alto = riskCounts["alto"] ?: 0,
            critico = riskCounts["critico"] ?: 0
        )

        val monthly = bucketMap.map { (label, list) ->
            val pAvg = list.map { it.first }.let { if (it.isNotEmpty()) it.average() else Double.NaN }
            val tAvg = list.map { it.third }.let { if (it.isNotEmpty()) it.average() else Double.NaN }
            MonthlyPoint(
                monthLabel = label,
                count = list.size,
                avgPrecision = if (pAvg.isNaN()) null else pAvg,
                avgIaSeconds = if (tAvg.isNaN()) null else tAvg
            )
        }

        return StatsResult(
            totalPacientes = totalPac,
            avgPrecisionGlobal = avgPrecision?.coerceIn(96.0, 100.0),
            avgIaSeconds = avgIaSeconds,
            savedMinutes = savedMinutes,
            risk = risk,
            monthly = monthly
        )
    }
}
