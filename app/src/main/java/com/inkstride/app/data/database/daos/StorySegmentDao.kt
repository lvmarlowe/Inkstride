package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.StorySegment

/**
 * Defines database access methods for StorySegment entities.
 *
 * Provides filtered lookups by id, milestone, and unlock/read state,
 * plus insert operations for single and bulk segment persistence.
 * All retrieval methods return segments in ascending id order for
 * deterministic narrative sequencing across sessions.
 */
@Dao
interface StorySegmentDao {

    // Returns a single segment by primary key, or null if not found.
    @Query("SELECT * FROM story_segment WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): StorySegment?

    // Returns all segments for a milestone, ordered by id.
    @Query("SELECT * FROM story_segment WHERE milestoneId = :milestoneId ORDER BY id ASC")
    suspend fun getByMilestoneId(milestoneId: Int): List<StorySegment>

    // Returns every segment in the local catalog, ordered by id.
    @Query("SELECT * FROM story_segment ORDER BY id ASC")
    suspend fun getAll(): List<StorySegment>

    // Returns unlocked, read segments ordered by milestone distance for the recap view.
    @Query(
        """
        SELECT s.*
        FROM story_segment s
        INNER JOIN unlock_state u ON u.storySegmentId = s.id
        INNER JOIN milestone m ON m.id = s.milestoneId
        WHERE u.unlocked = 1 AND u.read = 1
        ORDER BY m.distanceMarker ASC
        """
    )
    suspend fun getReadUnlockedOrderedByDistance(): List<StorySegment>

    // Returns unlocked, unread segments ordered by milestone distance for the story inbox.
    @Query(
        """
        SELECT s.*
        FROM story_segment s
        INNER JOIN unlock_state u ON u.storySegmentId = s.id
        INNER JOIN milestone m ON m.id = s.milestoneId
        WHERE u.unlocked = 1 AND u.read = 0
        ORDER BY m.distanceMarker ASC
        """
    )
    suspend fun getUnlockedUnreadOrderedByDistance(): List<StorySegment>

    // Inserts or replaces a single segment and returns the row id.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storySegment: StorySegment): Long

    // Inserts or replaces a collection of segments.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(storySegments: List<StorySegment>)
}