package com.example.contadorhoras.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore singleton por contexto
val Context.settingsDataStore by preferencesDataStore(name = "settings")

private object PrefKeys {
    val MONTHLY_LIMIT_MIN = intPreferencesKey("monthly_limit_min")
}

class SettingsRepository(private val context: Context) {

    /** Límite mensual en minutos (0 = sin límite). */
    val monthlyLimitMin: Flow<Int> =
        context.settingsDataStore.data.map { prefs -> prefs[PrefKeys.MONTHLY_LIMIT_MIN] ?: 0 }

    suspend fun setMonthlyLimitMin(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[PrefKeys.MONTHLY_LIMIT_MIN] = value.coerceAtLeast(0)
        }
    }
}
