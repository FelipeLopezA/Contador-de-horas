// prefs/SettingsDataStore.kt
package com.example.contadorhoras.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.map

// Usamos la extensión global:  import com.example.contadorhoras.prefs.dataStore

private val NORMAL_DAY_MIN = intPreferencesKey("normal_day_minutes")       // default 480
private val DEFAULT_PAUSE_MIN = intPreferencesKey("default_pause_minutes") // default 0

class SettingsDataStore(private val context: Context) {

    val normalDayMinutes = context.dataStore.data.map { it[NORMAL_DAY_MIN] ?: 8 * 60 }
    val defaultPauseMinutes = context.dataStore.data.map { it[DEFAULT_PAUSE_MIN] ?: 0 }

    suspend fun setNormalDayMinutes(min: Int) {
        context.dataStore.edit { it[NORMAL_DAY_MIN] = min.coerceAtLeast(0) }
    }

    suspend fun setDefaultPauseMinutes(min: Int) {
        context.dataStore.edit { it[DEFAULT_PAUSE_MIN] = min.coerceAtLeast(0) }
    }
}
