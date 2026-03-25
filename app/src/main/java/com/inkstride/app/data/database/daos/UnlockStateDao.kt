package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.UnlockState

/**
 * Defines database access methods for UnlockState entities.
 *
 * Provides segment-level unlock and read-state lookups,
 * unread existence checks, state updates, and upsert operations
 * for single and bulk unlock-state persistence.
 */
@Dao
interface UnlockStateDao {

    // Returns unlock state for a segment, or null if no state exists.
    @Query("SELECT * FROM unlock_state WHERE storySegmentId = :storySegmentId LIMIT 1")
    suspend fun getByStorySegmentId(storySegmentId: Int): UnlockState?

    // Returns all unlocked states ordered by segment id.
    @Query("SELECT * FROM unlock_state WHERE unlocked = 1 ORDER BY storySegmentId ASC")
    suspend fun getAllUnlocked(): List<UnlockState>

    // Returns true when at least one unlocked segment remains unread.
    @Query("SELECT COUNT(*) > 0 FROM unlock_state WHERE unlocked = 1 AND read = 0")
    suspend fun hasAnyUnlockedUnread(): Boolean

    // Inserts or replaces a single unlock state row.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unlockState: UnlockState)

    // Marks a segment as read.
    @Query("UPDATE unlock_state SET read = 1 WHERE storySegmentId = :storySegmentId")
    suspend fun markAsRead(storySegmentId: Int)

    // Deletes every unlock state row.
    @Query("DELETE FROM unlock_state")
    suspend fun deleteAll()

    // Inserts or replaces a collection of unlock state rows.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<UnlockState>)
}