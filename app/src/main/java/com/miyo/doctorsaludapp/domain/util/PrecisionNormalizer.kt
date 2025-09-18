package com.miyo.doctorsaludapp.domain.util

object PrecisionNormalizer {
    /** Acepta 0..1, 0..100 o "97.9%" y devuelve 96.0..100.0 */
    fun normalizeTo96_100(raw: Any?): Double? {
        val p = when (raw) {
            null -> return null
            is Number -> raw.toDouble()
            is String -> raw.trim().removeSuffix("%").toDoubleOrNull()
            else -> null
        } ?: return null
        val pct = if (p <= 1.0) p * 100.0 else p
        return pct.coerceIn(96.0, 100.0)
    }
}
