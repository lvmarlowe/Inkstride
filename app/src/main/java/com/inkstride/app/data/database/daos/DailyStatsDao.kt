package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.DailyStats

/**
 * DailyStatsDao: Defines database access methods for DailyStats entities.
 * Provides date-key lookups and ordered catalog reads,
 * plus upsert operations for daily stats persistence.
 */
@Dao
interface DailyStatsDao {

    // getByDate: Returns stats for one date key, or null if no record exists for that day.
    @Query("SELECT * FROM daily_stats WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): DailyStats?

    // getAll: Returns all daily stats ordered by date key for chronological processing.
    @Query("SELECT * FROM daily_stats ORDER BY dateKey ASC")
    suspend fun getAll(): List<DailyStats>

    // upsert: Inserts or replaces one daily stats record to keep totals current.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyStats: DailyStats)
}