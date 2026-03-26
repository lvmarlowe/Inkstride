package com.inkstride.app.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * ReadStepsWorker: Runs background step sync via WorkManager.
 * Delegates to StepSyncCoordinator and maps sync results to WorkManager return values.
 */
class ReadStepsWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    // doWork: Triggers a background sync and returns retry only when the sync fails and indicates it should retry.
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