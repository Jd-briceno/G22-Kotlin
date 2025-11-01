package com.g22.orbitsoundkotlin.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.g22.orbitsoundkotlin.data.workers.InterestsSyncWorker
import com.g22.orbitsoundkotlin.data.workers.ProfileReconciliationWorker
import com.g22.orbitsoundkotlin.data.workers.SyncWorker
import com.g22.orbitsoundkotlin.data.workers.TelemetrySyncWorker
import java.util.concurrent.TimeUnit

/**
 * Orquesta la sincronización de todos los workers de WorkManager.
 * Programa trabajos periódicos y one-time basados en eventos.
 */
class SyncManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val SYNC_WORK_NAME = "periodic_sync_work"
        private const val TELEMETRY_SYNC_WORK_NAME = "telemetry_sync_work"
        private const val RECONCILIATION_WORK_NAME = "profile_reconciliation_work"
        private const val INTERESTS_SYNC_WORK_NAME = "interests_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    /**
     * Inicializa todos los workers periódicos.
     * Debe llamarse al iniciar la app.
     */
    fun startPeriodicSync() {
        // Worker general de sincronización cada 15 min
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )

        // Worker de telemetría (más frecuente, prioritario)
        val telemetryWork = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(
            5,
            TimeUnit.MINUTES,
            2,
            TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            TELEMETRY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            telemetryWork
        )
    }

    /**
     * Programa un worker de reconciliación de perfil (one-time, solo cuando es necesario).
     */
    fun scheduleProfileReconciliation() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val reconciliationWork = OneTimeWorkRequestBuilder<ProfileReconciliationWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(reconciliationWork)
    }

    /**
     * Programa un worker de sincronización de intereses (one-time, solo cuando es necesario).
     */
    fun scheduleInterestsSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val interestsWork = OneTimeWorkRequestBuilder<InterestsSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(interestsWork)
    }

    /**
     * Cancela todos los workers programados (útil para testing o logout).
     */
    fun cancelAllWork() {
        workManager.cancelAllWork()
    }
}

