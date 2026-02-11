package com.inkstride.app.health

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val STEPS_SYNC_WORK = "steps_sync_work"

object StepsSyncScheduler {

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<ReadStepsWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            STEPS_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }
}
