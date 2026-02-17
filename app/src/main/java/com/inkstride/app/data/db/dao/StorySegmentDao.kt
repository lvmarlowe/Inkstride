package com.inkstride.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.db.entities.StorySegment

@Dao
interface StorySegmentDao {
    @Query("SELECT * FROM story_segment WHERE id = :id lIMIT 1")
    suspend fun getById(id: Int): StorySegment?

    @Query("SELECT * FROM story_segment WHERE milestoneId = :milestoneId ORDER BY id ASC")
    suspend fun getByMilestoneId(milestoneId: Int): List<StorySegment>

    @Query ("SELECT * FROM story_segment ORDER BY id ASC")
    suspend fun getAll(): List<StorySegment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storySegment: StorySegment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(storySegments: List<StorySegment>)
}