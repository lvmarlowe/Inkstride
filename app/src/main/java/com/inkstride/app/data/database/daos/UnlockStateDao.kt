package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.UnlockState

/**
 * UnlockStateDao: Defines database access methods for UnlockState entities.
 * Provides segment-level unlock and read-state lookups,
 * unread existence checks, state updates, and upsert operations
 * for single and bulk unlock-state persistence.
 */
@Dao
interface UnlockStateDao {

    // getByStorySegmentId: Returns unlock state for a segment, or null if no state exists.
    @Query("SELECT * FROM unlock_state WHERE storySegmentId = :storySegmentId LIMIT 1")
    suspend fun getByStorySegmentId(storySegmentId: Int): UnlockState?

    // getAllUnlocked: Returns all unlocked states ordered by segment id for sequential processing.
    @Query("SELECT * FROM unlock_state WHERE unlocked = 1 ORDER BY storySegmentId ASC")
    suspend fun getAllUnlocked(): List<UnlockState>

    // hasAnyUnlockedUnread: Returns true when at least one unlocked segment remains unread to drive inbox badge state.
    @Query("SELECT COUNT(*) > 0 FROM unlock_state WHERE unlocked = 1 AND read = 0")
    suspend fun hasAnyUnlockedUnread(): Boolean

    // getFurthestUnlockedDistance: Returns the greatest milestone distance among unlocked story segments.
    @Query(
        """
        SELECT MAX(m.distanceMarker)
        FROM unlock_state u
        INNER JOIN story_segment s ON s.id = u.storySegmentId
        INNER JOIN milestone m ON m.id = s.milestoneId
        WHERE u.unlocked = 1
        """
    )
    suspend fun getFurthestUnlockedDistance(): Double?

    // upsert: Inserts or replaces a single unlock state row to persist segment progress.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unlockState: UnlockState)

    // markAsRead: Marks a segment as read to clear it from the story inbox.
    @Query("UPDATE unlock_state SET read = 1 WHERE storySegmentId = :storySegmentId")
    suspend fun markAsRead(storySegmentId: Int)

    // deleteAll: Deletes every unlock state row to reset story progress.
    @Query("DELETE FROM unlock_state")
    suspend fun deleteAll()

    // upsertAll: Inserts or replaces a collection of unlock state rows for bulk state restoration.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<UnlockState>)
}