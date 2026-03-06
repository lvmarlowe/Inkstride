package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.StorySegment

@Dao
interface StorySegmentDao {
    @Query("SELECT * FROM story_segment WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): StorySegment?

    @Query("SELECT * FROM story_segment WHERE milestoneId = :milestoneId ORDER BY id ASC")
    suspend fun getByMilestoneId(milestoneId: Int): List<StorySegment>

    @Query("SELECT * FROM story_segment ORDER BY id ASC")
    suspend fun getAll(): List<StorySegment>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storySegment: StorySegment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(storySegments: List<StorySegment>)
}