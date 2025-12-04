package com.miyo.doctorsaludapp.domain.util

import java.util.*

fun Date.atStartOfDay(): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

fun Date.atEndOfDay(): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.time
}

fun monthsAgo(n: Int): Date {
    val c = Calendar.getInstance()
    c.add(Calendar.MONTH, -n)
    return c.time
}
