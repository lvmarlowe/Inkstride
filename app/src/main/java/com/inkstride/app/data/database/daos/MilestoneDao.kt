package com.inkstride.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkstride.app.data.database.entities.Milestone

/**
 * Defines database access methods for Milestone entities.
 *
 * Provides direct and distance-based milestone lookups,
 * area-name projection queries tied to story segments,
 * progression boundary queries, and upsert/delete operations
 * for single and bulk milestone persistence.
 */
@Dao
interface MilestoneDao {

    // Returns a single milestone by primary key, or null if not found.
    @Query("SELECT * FROM milestone WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Milestone?

    // Returns a milestone by exact distance marker, or null if not found.
    @Query("SELECT * FROM milestone WHERE distanceMarker = :distanceMarker LIMIT 1")
    suspend fun getByDistanceMarker(distanceMarker: Double): Milestone?

    // Returns all milestones ordered by route distance.
    @Query("SELECT * FROM milestone ORDER BY distanceMarker ASC")
    suspend fun getAll(): List<Milestone>

    // Returns area name for a story segment via milestone relationship.
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

    // Returns the farthest unlocked persistent area name for resume context.
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

    // Returns the latest milestone at or below current distance.
    @Query("SELECT * FROM milestone WHERE distanceMarker <= :currentDistance ORDER BY distanceMarker DESC LIMIT 1")
    suspend fun getLatestReached(currentDistance: Double): Milestone?

    // Returns the next milestone above current distance.
    @Query("SELECT * FROM milestone WHERE distanceMarker > :currentDistance ORDER BY distanceMarker ASC LIMIT 1")
    suspend fun getNextUnreached(currentDistance: Double): Milestone?

    // Inserts or replaces a single milestone and returns the row id.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: Milestone): Long

    // Inserts or replaces a collection of milestones.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<Milestone>)

    // Deletes milestones not included in the provided distance marker set.
    @Query("DELETE FROM milestone WHERE distanceMarker NOT IN (:distanceMarkers)")
    suspend fun deleteByDistanceMarkerNotIn(distanceMarkers: List<Double>)
}