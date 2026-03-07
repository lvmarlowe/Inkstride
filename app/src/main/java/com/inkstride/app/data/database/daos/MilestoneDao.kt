package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.Milestone

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestone WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Milestone?

    @Query("SELECT * FROM milestone WHERE distanceMarker = :distanceMarker LIMIT 1")
    suspend fun getByDistanceMarker(distanceMarker: Double): Milestone?

    @Query("SELECT * FROM milestone ORDER BY distanceMarker ASC")
    suspend fun getAll(): List<Milestone>

    @Query(
        """
        SELECT m.area_name
        FROM milestone m
        INNER JOIN story_segment s ON s.milestoneId = m.id
        WHERE s.id = :storySegmentId
        LIMIT 1
        """
    )
    suspend fun getAreaNameByStorySegmentId(storySegmentId: Int): String?

    @Query("SELECT * FROM milestone WHERE distanceMarker <= :currentDistance ORDER BY distanceMarker DESC LIMIT 1")
    suspend fun getLatestReached(currentDistance: Double): Milestone?

    @Query("SELECT * FROM milestone WHERE distanceMarker > :currentDistance ORDER BY distanceMarker ASC LIMIT 1")
    suspend fun getNextUnreached(currentDistance: Double): Milestone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: Milestone): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<Milestone>)
}