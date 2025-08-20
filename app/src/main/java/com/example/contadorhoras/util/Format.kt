package com.example.contadorhoras.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode
val APP_ZONE: java.time.ZoneId = java.time.ZoneId.of("America/Santiago")


private val HHmmFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val YMDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

// ms epoch -> "HH:mm" en zona de Chile
fun formatHHmm(ms: Long?): String =
    if (ms == null) "" else Instant.ofEpochMilli(ms).atZone(APP_ZONE).format(HHmmFormatter)

// Alias usado por CSVF
fun formatTimeMs(ms: Long?): String = formatHHmm(ms)

// Duración (ms) -> "HH:mm"
fun formatTotal(ms: Long): String {
    val totalMin = (ms / 60_000).toInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return "%02d:%02d".format(h, m)
}

// epochDay -> "yyyy-MM-dd" (no depende de zona)
fun formatDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(YMDFormatter)
fun formatHoursDecimalFromMinutes(minutes: Int, decimals: Int = 2): String {
    val hours = BigDecimal(minutes).divide(BigDecimal(60), decimals, RoundingMode.HALF_UP)
    return String.format(Locale.getDefault(), "%.${decimals}f h", hours)
}
fun formatHmsUnlimited(ms: Long): String {
    val safe = if (ms < 0) 0L else ms
    val totalSec = safe / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%d:%02d:%02d".format(h, m, s)
}
