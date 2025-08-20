package com.example.contadorhoras.util

import java.time.YearMonth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val LOCALE_ES_CL: Locale = Locale("es", "CL")

private val YM_FORMAT_ES = DateTimeFormatter.ofPattern("LLLL yyyy", LOCALE_ES_CL)   // ej: "agosto 2025"
private val DATE_LONG_ES = DateTimeFormatter.ofPattern("d 'de' LLLL yyyy", LOCALE_ES_CL) // "15 de agosto 2025"

fun formatYearMonthEs(ym: YearMonth): String =
    ym.atDay(1).format(YM_FORMAT_ES)

fun formatDateLongEs(date: LocalDate): String =
    date.format(DATE_LONG_ES)
