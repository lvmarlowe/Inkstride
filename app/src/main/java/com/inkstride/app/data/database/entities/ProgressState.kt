package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores aggregate journey progress for the active profile.
 *
 * Table uses a fixed primary key value so the app reads and writes
 * a single progress snapshot row.
 */
@Entity(tableName = "progress_state")
data class ProgressState(

    // Fixed row id.
    @PrimaryKey val id: Int = 1,

    // Stores current in-app day number.
    val dayNumber: Int = 1,

    // Stores lifetime step count for the profile.
    val totalSteps: Long = 0L,

    // Stores lifetime distance in the app's base distance unit.
    val totalDistance: Double = 0.0,

    // Stores last sync time as Unix epoch milliseconds.
    val lastSyncEpochMilliseconds: Long = 0L
)