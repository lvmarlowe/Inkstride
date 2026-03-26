package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.Milestone

/**
 * MilestoneDao: Defines database access methods for Milestone entities.
 * Provides direct and distance-based milestone lookups,
 * area-name projection queries tied to story segments,
 * progression boundary queries, and upsert/delete operations
 * for single and bulk milestone persistence.
 */
@Dao
interface MilestoneDao {

    // getById: Returns a single milestone by primary key, or null if not found.
    @Query("SELECT * FROM milestone WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Milestone?

    // getByDistanceMarker: Returns a milestone by exact distance marker, or null if not found.
    @Query("SELECT * FROM milestone WHERE distanceMarker = :distanceMarker LIMIT 1")
    suspend fun getByDistanceMarker(distanceMarker: Double): Milestone?

    // getAll: Returns all milestones ordered by route distance for sequential processing.
    @Query("SELECT * FROM milestone ORDER BY distanceMarker ASC")
    suspend fun getAll(): List<Milestone>

    // getAreaNameByStorySegmentId: Joins milestone to story segment to resolve the display area name.
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

    // getLatestPersistentUnlockedAreaName: Returns the farthest unlocked persistent area name for resume context.
    @Query(
        """
        SELECT m.area_name
        FROM milestone m
        INNER JOIN story_segment s ON s.milestoneId = m.id
        INNER JOIN unlock_state u ON u.storySegmentId = s.id
        WHERE u.unlocked = 1
          AND m.is_persistent = 1
        ORDER BY m.distanceMarker DESC
        LIMIT 1
        """
    )
    suspend fun getLatestPersistentUnlockedAreaName(): String?

    // getLatestReached: Returns the nearest milestone at or below current distance to track progress.
    @Query("SELECT * FROM milestone WHERE distanceMarker <= :currentDistance ORDER BY distanceMarker DESC LIMIT 1")
    suspend fun getLatestReached(currentDistance: Double): Milestone?

    // getNextUnreached: Returns the next milestone above current distance to display the upcoming goal.
    @Query("SELECT * FROM milestone WHERE distanceMarker > :currentDistance ORDER BY distanceMarker ASC LIMIT 1")
    suspend fun getNextUnreached(currentDistance: Double): Milestone?

    // insert: Inserts or replaces a single milestone and returns the row id for reference.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: Milestone): Long

    // insertAll: Inserts or replaces a collection of milestones for bulk route setup.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<Milestone>)

    // deleteByDistanceMarkerNotIn: Deletes milestones not included in the provided distance marker set to sync route changes.
    @Query("DELETE FROM milestone WHERE distanceMarker NOT IN (:distanceMarkers)")
    suspend fun deleteByDistanceMarkerNotIn(distanceMarkers: List<Double>)
}