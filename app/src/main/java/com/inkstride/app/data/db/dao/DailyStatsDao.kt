package com.inkstride.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.db.entities.DailyStats

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): DailyStats?

    @Query("SELECT * FROM daily_stats ORDER BY dateKey ASC")
    suspend fun getAll(): List<DailyStats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyStats: DailyStats)

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAll()
}