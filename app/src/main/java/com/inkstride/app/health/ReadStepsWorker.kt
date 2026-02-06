package com.inkstride.app.health

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReadStepsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val healthConnectManager = HealthConnectManager(applicationContext)

            val end = Instant.now()
            val start = end.minus(24, ChronoUnit.HOURS)
            val totalSteps = healthConnectManager.readTotalSteps(start, end)

            Log.i(TAG, "Background read: $totalSteps steps in last 24 hours")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ReadStepsWorker"
    }
}