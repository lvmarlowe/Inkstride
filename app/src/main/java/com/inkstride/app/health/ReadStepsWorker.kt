package com.inkstride.app.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReadStepsWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = try {
        StepsSyncer.syncIfPermitted(applicationContext)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
