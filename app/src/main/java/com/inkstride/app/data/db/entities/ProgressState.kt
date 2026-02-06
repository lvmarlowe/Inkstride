package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_state")
data class ProgressState(
    @PrimaryKey val id: Int = 1,

    val dayNumber: Int = 1,
    val totalSteps: Long = 0L,
    val totalMiles: Double = 0.0,
    val lastSyncEpochMilliseconds: Long = 0L
)