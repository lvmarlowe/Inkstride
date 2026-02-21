package com.inkstride.app.data.repository

import android.content.Context
import com.inkstride.app.data.db.dao.DailyStatsDao
import com.inkstride.app.data.db.dao.ProgressStateDao
import com.inkstride.app.data.db.entities.DailyStats
import com.inkstride.app.data.db.entities.ProgressState
import com.inkstride.app.health.StepTotals
import com.inkstride.app.services.ProgressCalculator
import java.time.LocalDate

class ProgressRepository(
    private val context: Context,
    private val progressStateDao: ProgressStateDao,
    private val dailyStatsDao: DailyStatsDao
) {
    private val progressCalculator = ProgressCalculator()

    suspend fun persistSnapshotFromHealthConnect(
        stepTotals: StepTotals,
        dayNumber: Int
    ): Double {
        val totalDistance = progressCalculator.stepsToDistance(stepTotals.cumulativeSteps)
        val todayDistance = progressCalculator.stepsToDistance(stepTotals.todaySteps)

        val updatedState = ProgressState(
            id = 1,
            dayNumber = dayNumber,
            totalSteps = stepTotals.cumulativeSteps,
            totalDistance = progressCalculator.roundDistance(totalDistance),
            lastSyncEpochMilliseconds = System.currentTimeMillis()
        )
        progressStateDao.upsert(updatedState)

        val dateKey = LocalDate.now().toString()
        val todayStats = DailyStats(
            dateKey = dateKey,
            stepsToday = stepTotals.todaySteps,
            distanceToday = progressCalculator.roundDistance(todayDistance)
        )
        dailyStatsDao.upsert(todayStats)

        return progressCalculator.roundDistance(totalDistance)
    }
}