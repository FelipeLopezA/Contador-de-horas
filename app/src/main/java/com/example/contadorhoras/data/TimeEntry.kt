package com.example.contadorhoras.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,     // LocalDate.now().toEpochDay()
    val startMillis: Long,
    val endMillis: Long? = null,
    val pauseMinutes: Int = 0
)
