package com.example.contadorhoras

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contadorhoras.data.AppDatabase
import com.example.contadorhoras.data.TimeEntry
import com.example.contadorhoras.data.TimeRepository
import com.example.contadorhoras.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.contadorhoras.data.SettingsRepository

data class InProgress(val startMillis: Long)

data class MonthSummary(
    val totalMs: Long,
    val daysWorked: Int
)

class TimeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TimeRepository(AppDatabase.get(app).timeDao())

    var inProgress by mutableStateOf<InProgress?>(null)
        private set

    // ⏱️ “reloj” interno para hacer tick del cronómetro cada segundo
    var now by mutableStateOf(System.currentTimeMillis())
    init {
        viewModelScope.launch {
            while (true) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() }
        }
    }
    fun start() {
        if (inProgress == null) {
            viewModelScope.launch {
                repo.startNow(defaultPauseMin = 0) // ignoramos pausas
                inProgress = InProgress(System.currentTimeMillis())
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            repo.stopNow()
            inProgress = null
        }
    }

    fun updateTimes(entry: TimeEntry, startMillis: Long, endMillis: Long?) =
        viewModelScope.launch {
            repo.update(entry.copy(startMillis = startMillis, endMillis = endMillis))
        }

    fun dayEntry(date: java.time.LocalDate) = repo.observeDay(date.toEpochDay())

    fun entriesForMonth(ym: YearMonthX): Flow<List<TimeEntry>> =
        repo.observeRange(ym.firstDayEpoch(), ym.lastDayEpoch())

    fun monthSummary(entries: List<TimeEntry>): MonthSummary {
        var total = 0L
        val uniqueDays = mutableSetOf<Long>()

        for (e in entries) {
            val end = e.endMillis ?: continue
            val ms = (end - e.startMillis).coerceAtLeast(0L)
            if (ms > 0L) {
                total += ms
                uniqueDays += e.dateEpochDay
            }
        }
        return MonthSummary(totalMs = total, daysWorked = uniqueDays.size)
    }
    suspend fun csvFor(ym: YearMonthX): String {
        val rows = repo.getRangeOnce(ym.firstDayEpoch(), ym.lastDayEpoch())
        val sb = StringBuilder().appendLine("Fecha,Inicio,Fin,Total(hh:mm:ss)")
        for (r in rows) {
            val total = if (r.endMillis != null) {
                val ms = ((r.endMillis - r.startMillis)).coerceAtLeast(0)
                com.example.contadorhoras.util.formatHmsUnlimited(ms)
            } else ""
            sb.appendLine("${formatDate(r.dateEpochDay)},${formatTimeMs(r.startMillis)},${formatTimeMs(r.endMillis)},$total")
        }
        return sb.toString()
    }

    fun createEntryForDay(date: java.time.LocalDate, startMillis: Long, endMillis: Long?) =
        viewModelScope.launch {
            repo.createForDay(date.toEpochDay(), startMillis, endMillis)
        }
    fun dayEntries(date: java.time.LocalDate): Flow<List<TimeEntry>> =
        repo.observeDayList(date.toEpochDay())

    private val settingsRepo = SettingsRepository(app)
    val monthlyLimitMin: Flow<Int> = settingsRepo.monthlyLimitMin
    fun updateMonthlyLimitMin(value: Int) = viewModelScope.launch {
        settingsRepo.setMonthlyLimitMin(value)
    }
}
