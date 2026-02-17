package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val dateKey: String,
    val stepsToday: Long = 0L,
    val distanceToday: Double = 0.0
)
