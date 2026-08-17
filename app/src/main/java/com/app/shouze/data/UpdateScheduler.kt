package com.app.shouze.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UpdateScheduler {
    private const val UNIQUE_WORK = "shouze_update_check"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun apply(context: Context, frequency: UpdateFrequency) {
        val workManager = WorkManager.getInstance(context)
        when (frequency) {
            UpdateFrequency.EVERY_LAUNCH -> {
                workManager.cancelUniqueWork(UNIQUE_WORK)
                workManager.enqueueUniqueWork(
                    UNIQUE_WORK,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                        .setConstraints(networkConstraint)
                        .build()
                )
            }
            UpdateFrequency.WEEKLY -> {
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequestBuilder<UpdateCheckWorker>(7, TimeUnit.DAYS)
                        .setConstraints(networkConstraint)
                        .build()
                )
            }
            UpdateFrequency.BI_WEEKLY -> {
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    PeriodicWorkRequestBuilder<UpdateCheckWorker>(14, TimeUnit.DAYS)
                        .setConstraints(networkConstraint)
                        .build()
                )
            }
            UpdateFrequency.NEVER -> workManager.cancelUniqueWork(UNIQUE_WORK)
        }
    }
}