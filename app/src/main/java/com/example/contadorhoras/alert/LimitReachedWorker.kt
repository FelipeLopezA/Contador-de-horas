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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.contadorhoras.MainActivity
import com.example.contadorhoras.R
import com.example.contadorhoras.data.AppDatabase
import com.example.contadorhoras.data.TimeRepository
import com.example.contadorhoras.prefs.KEY_LIMIT_ALERT_SENT_FOR
import com.example.contadorhoras.prefs.KEY_MONTHLY_LIMIT_MIN
import com.example.contadorhoras.prefs.dataStore
import com.example.contadorhoras.util.YearMonthX
import com.example.contadorhoras.util.formatHmsUnlimited
import com.example.contadorhoras.util.nowYearMonth
import kotlinx.coroutines.flow.first
import java.time.YearMonth

class LimitReachedWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = TimeRepository(AppDatabase.get(appContext).timeDao())

        // Límite configurado
        val prefs = appContext.dataStore.data.first()
        val limitMin = prefs[KEY_MONTHLY_LIMIT_MIN] ?: 0
        if (limitMin <= 0) return Result.success()

        // Total del mes actual
        val ym: YearMonthX = nowYearMonth()
        val rows = repo.getRangeOnce(ym.firstDayEpoch(), ym.lastDayEpoch())

        // Sumar en LONG y convertir al final (evita ambigüedad de tipos)
        val totalMin: Int = rows.sumOf { r ->
            val end = r.endMillis ?: return@sumOf 0L
            val ms = (end - r.startMillis).coerceAtLeast(0)
            (ms / 60_000L)  // minutos de este registro (Long)
        }.toInt()

        // ¿Alcanzó o superó el límite?
        if (totalMin >= limitMin) {
            val tag = YearMonth.of(ym.year, ym.month).toString() // "YYYY-MM"
            val already = prefs[KEY_LIMIT_ALERT_SENT_FOR]
            if (already != tag) {
                sendLimitReachedNotification(totalMin, limitMin)
                appContext.dataStore.edit { it[KEY_LIMIT_ALERT_SENT_FOR] = tag }
            }
        }

        return Result.success()
    }

    private fun sendLimitReachedNotification(totalMin: Int, limitMin: Int) {
        // Permiso en Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return

        ensureChannel()

        val intent = Intent(appContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val workedText = formatHmsUnlimited(totalMin * 60_000L)
        val limitText  = formatHmsUnlimited(limitMin * 60_000L)
        val text = "Límite mensual alcanzado. Trabajado: $workedText • Límite: $limitText."

        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_splash) // ícono simple/monocromo
            .setContentTitle("¡Alcanzaste tu límite mensual!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(2001, notif)
    }

    private fun ensureChannel() {
        // Tu minSdk es 26, esto siempre se ejecuta
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de límite mensual",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "hours_limit_channel"
    }
}
