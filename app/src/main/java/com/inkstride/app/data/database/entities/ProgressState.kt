package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ProgressState: Stores aggregate journey progress for the active profile.
 * Table uses a fixed primary key value so the app reads and writes
 * a single progress snapshot row.
 */
@Entity(tableName = "progress_state")
data class ProgressState(

    // Fixed row id keeps progress contained to one row per profile.
    @PrimaryKey val id: Int = 1,

    // Stores current in-app day number to drive narrative progression.
    val dayNumber: Int = 1,

    // Stores lifetime step count for the profile to track overall activity.
    val totalSteps: Long = 0L,

    // Stores cumulative offset added to Health Connect step reads to preserve
    // lifetime totals when Health Connect reports only a limited history window.
    val cumulativeOffsetSteps: Long = 0L,

    // Stores lifetime distance in the app's base distance unit for journey calculations.
    val totalDistance: Double = 0.0,

    // Stores last sync time as Unix epoch milliseconds to detect stale data on resume.
    val lastSyncEpochMilliseconds: Long = 0L
)