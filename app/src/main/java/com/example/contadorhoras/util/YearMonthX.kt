package com.example.contadorhoras.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class YearMonthX(val year: Int, val month: Int) {
    fun firstDay(): LocalDate = LocalDate.of(year, month, 1)
    fun lastDay(): LocalDate = firstDay().withDayOfMonth(firstDay().lengthOfMonth())
    fun firstDayEpoch(): Long = firstDay().toEpochDay()
    fun lastDayEpoch(): Long = lastDay().toEpochDay()
    fun plusMonths(delta: Long): YearMonthX {
        val ym = YearMonth.of(year, month).plusMonths(delta)
        return YearMonthX(ym.year, ym.monthValue)
    }
    override fun toString(): String =
        YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
}

fun nowYearMonth(): YearMonthX {
    val ym = YearMonth.now()
    return YearMonthX(ym.year, ym.monthValue)
}
