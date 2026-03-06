package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.UnlockState

@Dao
interface UnlockStateDao {
    @Query("SELECT * FROM unlock_state WHERE storySegmentId = :storySegmentId LIMIT 1")
    suspend fun getByStorySegmentId(storySegmentId: Int): UnlockState?

    @Query("SELECT * FROM unlock_state WHERE unlocked = 1 ORDER BY storySegmentId ASC")
    suspend fun getAllUnlocked(): List<UnlockState>

    @Query("SELECT COUNT(*) > 0 FROM unlock_state WHERE unlocked = 1 AND read = 0")
    suspend fun hasAnyUnlockedUnread(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unlockState: UnlockState)

    @Query("UPDATE unlock_state SET read = 1 WHERE storySegmentId = :storySegmentId")
    suspend fun markAsRead(storySegmentId: Int)

    @Query("DELETE FROM unlock_state")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<UnlockState>)
}