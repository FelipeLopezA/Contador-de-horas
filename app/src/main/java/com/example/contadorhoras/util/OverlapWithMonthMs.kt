package com.example.contadorhoras.util

import java.time.YearMonth

fun overlapWithMonthMs(startMs: Long, endMs: Long, ym: YearMonthX): Long {
    val ymJava = YearMonth.of(ym.year, ym.month)
    val monthStartMs = ymJava.atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli()
    val monthEndMs   = ymJava.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli()

    val s = startMs.coerceIn(monthStartMs, monthEndMs)
    val e = endMs.coerceIn(monthStartMs, monthEndMs)
    return (e - s).coerceAtLeast(0L)
}
