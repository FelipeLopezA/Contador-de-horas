// prefs/Prefs.kt
package com.example.contadorhoras.prefs

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Única extensión global:
val Context.dataStore by preferencesDataStore(name = "settings")

// Claves compartidas
val KEY_MONTHLY_LIMIT_MIN = intPreferencesKey("monthly_limit_min")
val KEY_LIMIT_ALERT_SENT_FOR = stringPreferencesKey("alert_limit_reached_for")
