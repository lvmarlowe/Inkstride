package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.Settings

/**
 * SettingsDao: Defines database access methods for Settings.
 * Provides read and upsert operations against the fixed
 * settings row stored at id 1.
 */
@Dao
interface SettingsDao {

    // get: Returns the single settings row, or null if not initialized.
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): Settings?

    // upsert: Inserts or replaces the single settings row to persist user preferences.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: Settings)
}