package com.inkstride.app.health

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Unique work name used to identify and update the periodic sync job in WorkManager.
private const val STEPS_SYNC_WORK = "steps_sync_work"

/**
 * StepsSyncScheduler: Schedules periodic background step syncs via WorkManager.
 * Uses UPDATE policy so rescheduling replaces the existing job without creating duplicates.
 */
object StepsSyncScheduler {

    // schedule: Enqueues a repeating 15-minute sync job, replacing any existing schedule on update.
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReadStepsWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            STEPS_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}