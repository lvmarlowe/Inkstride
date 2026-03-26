package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.ProgressState

/**
 * ProgressStateDao: Defines database access methods for ProgressState.
 * Provides read and upsert operations against the fixed
 * progress row stored at id 1.
 */
@Dao
interface ProgressStateDao {

    // get: Returns the single progress row, or null if not initialized.
    @Query("SELECT * FROM progress_state WHERE id = 1")
    suspend fun get(): ProgressState?

    // upsert: Inserts or replaces the single progress row to persist journey state.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ProgressState)
}