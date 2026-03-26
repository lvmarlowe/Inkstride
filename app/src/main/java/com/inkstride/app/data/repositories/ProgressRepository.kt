package com.inkstride.app.data.repositories

import android.content.Context
import com.inkstride.app.data.database.daos.DailyStatsDao
import com.inkstride.app.data.database.daos.ProgressStateDao
import com.inkstride.app.data.database.entities.DailyStats
import com.inkstride.app.data.database.entities.ProgressState
import com.inkstride.app.health.StepTotals
import com.inkstride.app.services.DataValidator
import com.inkstride.app.services.ProgressCalculator
import java.time.LocalDate

/**
 * ProgressRepository: Handles persistence of step and distance data from Health Connect.
 * Keeps progress state and daily stats in sync so journey calculations reflect current activity.
 */
class ProgressRepository(
    private val context: Context,
    private val progressStateDao: ProgressStateDao,
    private val dailyStatsDao: DailyStatsDao
) {
    // Applies shared coercion and normalization rules before values are persisted.
    private val dataValidator = DataValidator()

    // Applies shared distance and rounding rules for progress calculations.
    private val progressCalculator = ProgressCalculator()

    /**
     * persistSnapshotFromHealthConnect: Writes cumulative and daily totals from a Health Connect sync.
     * Returns the rounded total distance for use by the caller after persisting both rows.
     */
    suspend fun persistSnapshotFromHealthConnect(
        stepTotals: StepTotals,
        dayNumber: Int
    ): Double {
        val safeDayNumber = dataValidator.coerceDayNumber(dayNumber)
        val safeCumulativeSteps = dataValidator.coerceSteps(stepTotals.cumulativeSteps)
        val safeTodaySteps = dataValidator.coerceSteps(stepTotals.todaySteps)

        val totalDistance = progressCalculator.stepsToDistance(safeCumulativeSteps)
        val todayDistance = progressCalculator.stepsToDistance(safeTodaySteps)

        val updatedState = ProgressState(
            id = 1,
            dayNumber = safeDayNumber,
            totalSteps = safeCumulativeSteps,
            totalDistance = progressCalculator.roundDistance(totalDistance),
            lastSyncEpochMilliseconds = System.currentTimeMillis()
        )
        progressStateDao.upsert(updatedState)

        val dateKey = LocalDate.now().toString()
        val todayStats = DailyStats(
            dateKey = dateKey,
            stepsToday = safeTodaySteps,
            distanceToday = progressCalculator.roundDistance(todayDistance)
        )
        dailyStatsDao.upsert(todayStats)

        return progressCalculator.roundDistance(totalDistance)
    }
}