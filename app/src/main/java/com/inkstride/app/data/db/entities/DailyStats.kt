package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val dateKey: String,   // e.g., "2026-02-11"
    val stepsToday: Long = 0L,
    val milesToday: Double = 0.0
)
