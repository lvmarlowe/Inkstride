package com.inkstride.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.db.entities.ProgressState

@Dao
interface ProgressStateDao {
    @Query("SELECT * FROM progress_state WHERE id = 1 LIMIT 1")
    suspend fun get(): ProgressState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ProgressState)
}