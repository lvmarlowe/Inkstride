package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.inkstride.app.data.database.entities.ProgressState

/**
 * ProgressStateDao: Defines database access methods for ProgressState.
 * Provides read and upsert operations against the fixed
 * progress row stored at id 1.
 */
@Dao
interface ProgressStateDao {

    // get: Returns the single progress row, or null if not initialized.
    @Query("SELECT * FROM progress_state WHERE id = 1")
    suspend fun get(): ProgressState?

    // upsert: Inserts or replaces the single progress row to persist journey state.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ProgressState)

    // upsertFromHealthConnectPreservingStorybook: Writes sync-derived progress fields while preserving
    // the existing storybookLastSeenDistance value atomically to avoid lost updates from concurrent writes.
    @Transaction
    suspend fun upsertFromHealthConnectPreservingStorybook(
        dayNumber: Int,
        totalSteps: Long,
        cumulativeOffsetSteps: Long,
        totalDistance: Double,
        lastSyncEpochMilliseconds: Long
    ) {
        val existingStorybookLastSeenDistance = get()?.storybookLastSeenDistance ?: 0.0

        upsert(
            ProgressState(
                id = 1,
                dayNumber = dayNumber,
                totalSteps = totalSteps,
                cumulativeOffsetSteps = cumulativeOffsetSteps,
                totalDistance = totalDistance,
                lastSyncEpochMilliseconds = lastSyncEpochMilliseconds,
                storybookLastSeenDistance = existingStorybookLastSeenDistance
            )
        )
    }

    // updateStorybookLastSeenDistance: Persists Storybook's latest seen distance threshold.
    @Query("UPDATE progress_state SET storybookLastSeenDistance = :distance WHERE id = 1")
    suspend fun updateStorybookLastSeenDistance(distance: Double)
}