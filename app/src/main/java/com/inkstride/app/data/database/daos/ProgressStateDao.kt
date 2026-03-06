package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.ProgressState

@Dao
interface ProgressStateDao {
    @Query("SELECT * FROM progress_state WHERE id = 1")
    suspend fun get(): ProgressState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ProgressState)
}