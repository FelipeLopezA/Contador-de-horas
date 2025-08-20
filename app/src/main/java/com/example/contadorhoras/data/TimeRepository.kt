package com.example.contadorhoras.data

import kotlinx.coroutines.flow.Flow
import com.example.contadorhoras.util.APP_ZONE
import java.time.LocalDate

class TimeRepository(private val dao: TimeEntryDao) {


    suspend fun startNow(defaultPauseMin: Int = 0) {
        val now = System.currentTimeMillis()
        val day = LocalDate.now(APP_ZONE).toEpochDay()
        dao.insert(TimeEntry(dateEpochDay = day, startMillis = now, pauseMinutes = defaultPauseMin))
    }


    suspend fun stopNow() {
        dao.getOpen()?.let { dao.update(it.copy(endMillis = System.currentTimeMillis())) }
    }

    suspend fun getOpenOnce(): TimeEntry? = dao.getOpen()

    fun observeDay(epochDay: Long): Flow<TimeEntry?> = dao.observeByDay(epochDay)

    fun observeRange(fromDay: Long, toDay: Long): Flow<List<TimeEntry>> =
        dao.observeRange(fromDay, toDay)

    suspend fun getRangeOnce(fromDay: Long, toDay: Long): List<TimeEntry> =
        dao.getRangeOnce(fromDay, toDay)

    suspend fun createForDay(epochDay: Long, startMillis: Long, endMillis: Long?) {
        dao.insert(
            TimeEntry(
                dateEpochDay = epochDay,
                startMillis = startMillis,
                endMillis = endMillis,
                pauseMinutes = 0
            )
        )
    }

    suspend fun update(entry: TimeEntry) = dao.update(entry)

    fun observeDayList(epochDay: Long): Flow<List<TimeEntry>> = dao.observeListByDay(epochDay)
}
