package com.inkstride.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.db.entities.Settings

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: Settings)

    @Query("SELECT COUNT(*) FROM settings")
    suspend fun countRows(): Int
}