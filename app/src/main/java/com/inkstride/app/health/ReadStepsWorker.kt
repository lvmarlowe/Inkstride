package com.inkstride.app.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReadStepsWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        return when (val result = StepSyncCoordinator.syncNow(applicationContext, StepSyncTrigger.BACKGROUND)) {
            is StepSyncResult.Success,
            StepSyncResult.NoPermission,
            StepSyncResult.SkippedAlreadyRunning,
            StepSyncResult.QueuedForRerun -> Result.success()

            is StepSyncResult.Failure -> if (result.shouldRetry) Result.retry() else Result.success()
        }
    }
}
