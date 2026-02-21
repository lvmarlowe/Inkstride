package com.inkstride.app.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.repository.ProgressRepository
import com.inkstride.app.services.MilestoneEngine

class ReadStepsWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = try {
        val totals = StepsSyncer.syncIfPermitted(applicationContext) ?: return Result.success()

        val healthConnectManager = HealthConnectManager(applicationContext)
        val journeyStart = healthConnectManager.getJourneyStartInstant()
        val dayNumber = HealthConnectManager.computeDayNumberFromJourneyStart(journeyStart)

        val database = DatabaseProvider.getDatabase(applicationContext)
        val progressRepository = ProgressRepository(
            context = applicationContext,
            progressStateDao = database.progressStateDao(),
            dailyStatsDao = database.dailyStatsDao()
        )

        val totalDistance = progressRepository.persistSnapshotFromHealthConnect(
            stepTotals = totals,
            dayNumber = dayNumber
        )

        val milestoneEngine = MilestoneEngine(applicationContext)
        milestoneEngine.checkAndUnlockForDistance(totalDistance)

        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}