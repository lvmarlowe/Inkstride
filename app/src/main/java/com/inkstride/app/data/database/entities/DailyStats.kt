package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores daily activity totals for one calendar day.
 *
 * Each row uses a date key as the primary key,
 * so one day maps to one summary record.
 */
@Entity(tableName = "daily_stats")
data class DailyStats(

    // Uses normalized local date format (yyyy-mm-dd).
    @PrimaryKey val dateKey: String,

    // Stores total steps counted for the day.
    val stepsToday: Long = 0L,

    // Stores total distance for the day in the app's base distance unit.
    val distanceToday: Double = 0.0
)