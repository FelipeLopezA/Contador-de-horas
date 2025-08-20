package com.example.contadorhoras.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {

    // Si hubiera más de una abierta por error, tomamos la más reciente
    @Query("SELECT * FROM time_entries WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    suspend fun getOpen(): TimeEntry?

    @Insert
    suspend fun insert(entry: TimeEntry): Long

    @Update
    suspend fun update(entry: TimeEntry)


    @Query("SELECT * FROM time_entries WHERE dateEpochDay = :epochDay ORDER BY startMillis DESC LIMIT 1")
    fun observeByDay(epochDay: Long): Flow<TimeEntry?>

    @Query("SELECT * FROM time_entries WHERE dateEpochDay BETWEEN :fromDay AND :toDay ORDER BY dateEpochDay ASC, startMillis ASC")
    fun observeRange(fromDay: Long, toDay: Long): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE dateEpochDay BETWEEN :fromDay AND :toDay ORDER BY dateEpochDay ASC, startMillis ASC")
    suspend fun getRangeOnce(fromDay: Long, toDay: Long): List<TimeEntry>

    @Query("SELECT * FROM time_entries WHERE dateEpochDay = :epochDay ORDER BY startMillis ASC")
    fun observeListByDay(epochDay: Long): Flow<List<TimeEntry>>

}
