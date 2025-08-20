package com.example.contadorhoras.alert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.contadorhoras.MainActivity
import com.example.contadorhoras.R
import com.example.contadorhoras.data.AppDatabase
import com.example.contadorhoras.data.TimeRepository
import com.example.contadorhoras.util.YearMonthX
import com.example.contadorhoras.util.formatHmsUnlimited
import com.example.contadorhoras.util.nowYearMonth
import kotlinx.coroutines.flow.first
import java.time.YearMonth

val Context.dataStore by preferencesDataStore(name = "settings")
private val KEY_MONTHLY_LIMIT_MIN = intPreferencesKey("monthly_limit_min")
private val KEY_ALERT_SENT_FOR = stringPreferencesKey("alert_one_hour_sent_for") // "YYYY-MM"

class RemainingHourWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = TimeRepository(AppDatabase.get(appContext).timeDao())

        // Límite guardado
        val prefs = appContext.dataStore.data.first()
        val limitMin = prefs[KEY_MONTHLY_LIMIT_MIN] ?: 0
        if (limitMin <= 0) return Result.success()

        // Totales del mes actual
        val ym: YearMonthX = nowYearMonth()
        val rows = repo.getRangeOnce(ym.firstDayEpoch(), ym.lastDayEpoch())
        val totalMin = rows.sumOf { r ->
            val end = r.endMillis ?: return@sumOf 0
            val ms = (end - r.startMillis).coerceAtLeast(0)
            (ms / 60000).toInt()
        }

        val remaining = (limitMin - totalMin)
        if (remaining in 1..60) {
            // ¿Ya se notificó este mes?
            val tag = YearMonth.of(ym.year, ym.month).toString() // p.ej. "2025-08"
            val already = prefs[KEY_ALERT_SENT_FOR]
            if (already != tag) {
                sendNotification(remaining)
                appContext.dataStore.edit { it[KEY_ALERT_SENT_FOR] = tag }
            }
        }

        return Result.success()
    }

    private fun sendNotification(remainingMin: Int) {
        // 1) chequear permiso en Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return // sin permiso => no notificamos
        }

        // 2) chequear si el usuario desactivó notificaciones para la app
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return

        ensureChannel()

        val intent = Intent(appContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Queda ${formatHmsUnlimited(remainingMin * 60_000L)} para tu límite mensual."
        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_splash) // usa un ícono monocromo
            .setContentTitle("¡Te queda 1 hora o menos!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        // 3) intentar notificar
        try {
            NotificationManagerCompat.from(appContext).notify(1001, notif)
        } catch (_: SecurityException) {
            // por si algún OEM lanza SecurityException igualmente
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Límite de horas",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "hours_limit_channel"
    }
}
