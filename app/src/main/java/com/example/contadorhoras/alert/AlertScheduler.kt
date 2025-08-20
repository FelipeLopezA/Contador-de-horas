package com.example.contadorhoras.alert

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val UNIQUE_NAME = "limit_reached_alert_worker"

fun scheduleLimitChecks(context: Context) {
    val req = PeriodicWorkRequestBuilder<LimitReachedWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        req
    )
}
