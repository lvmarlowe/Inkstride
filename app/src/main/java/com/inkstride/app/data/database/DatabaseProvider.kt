package com.inkstride.app.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.inkstride.app.data.database.entities.Milestone
import com.inkstride.app.data.database.entities.Settings
import com.inkstride.app.data.database.entities.StorySegment
import com.inkstride.app.data.database.entities.UnlockState

/**
 * Creates and seeds the application database.
 */
object DatabaseProvider {
    @Volatile
    private var instance: InkstrideDatabase? = null

    /**
     * Returns a database instance for the process.
     */
    fun getDatabase(context: Context): InkstrideDatabase {
        return instance ?: synchronized(this) {
            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                InkstrideDatabase::class.java,
                "inkstride_database"
            )
                .addMigrations(
                    DatabaseMigrator.MIGRATION_6_7,
                    DatabaseMigrator.MIGRATION_7_8
                )
                .build()
            instance = newInstance
            newInstance
        }
    }

    /**
     * Ensures default settings and seeded story data exist.
     */
    suspend fun ensureDefaults(context: Context) {
        val database = getDatabase(context)
        ensureDefaultSettings(database)

        val characterName = database.settingsDao().get()?.normalized()?.characterName
            ?: Settings.DEFAULT_CHARACTER_NAME
        val seededStoryData = StorySeedDataSource.load(context, characterName)

        ensureSeededMilestonesAndStory(database, seededStoryData)
        ensureSeededUnlockStates(database, seededStoryData)
    }

    /**
     * Writes normalized default settings when stored settings are missing or invalid.
     */
    private suspend fun ensureDefaultSettings(database: InkstrideDatabase) {
        val settingsDao = database.settingsDao()
        val currentSettings = settingsDao.get()
        val normalizedSettings = (currentSettings ?: Settings()).normalized()

        // Avoids unnecessary writes when values are already normalized.
        if (currentSettings != normalizedSettings) {
            settingsDao.upsert(normalizedSettings)
        }
    }

    /**
     * Seeds milestones and story segments from asset data.
     */
    private suspend fun ensureSeededMilestonesAndStory(
        database: InkstrideDatabase,
        seededStoryData: List<StorySeedEntry>
    ) {
        // Keeps milestone and story updates in one transaction.
        database.withTransaction {
            val milestoneDao = database.milestoneDao()
            val storySegmentDao = database.storySegmentDao()

            // Removes seeded milestones that are no longer present in asset data.
            if (seededStoryData.isNotEmpty()) {
                milestoneDao.deleteByDistanceMarkerNotIn(
                    seededStoryData.map { it.distanceMarker }
                )
            }

            for ((distanceMarker, isPersistent, isMajor, areaName, text, _, _) in seededStoryData) {
                val milestone = milestoneDao.getByDistanceMarker(distanceMarker)
                val milestoneId = milestone?.id
                    ?: milestoneDao.insert(
                        Milestone(
                            distanceMarker = distanceMarker,
                            isPersistent = isPersistent,
                            isMajor = isMajor,
                            areaName = areaName
                        )
                    ).toInt()

                // Refreshes mutable milestone fields when seed values change.
                if (
                    milestone != null && (
                            milestone.areaName != areaName ||
                                    milestone.isMajor != isMajor ||
                                    milestone.isPersistent != isPersistent
                            )
                ) {
                    milestoneDao.insert(
                        milestone.copy(
                            areaName = areaName,
                            isMajor = isMajor,
                            isPersistent = isPersistent
                        )
                    )
                }

                val existingSegment = storySegmentDao.getByMilestoneId(milestoneId).firstOrNull()
                if (existingSegment == null) {
                    storySegmentDao.insert(
                        StorySegment(
                            milestoneId = milestoneId,
                            text = text
                        )
                    )
                } else if (existingSegment.text != text) {
                    // Refreshes segment text when seed text changes.
                    storySegmentDao.insert(existingSegment.copy(text = text))
                }
            }
        }
    }

    /**
     * Seeds unlock-state rows that are missing for existing story segments.
     */
    private suspend fun ensureSeededUnlockStates(
        database: InkstrideDatabase,
        seededStoryData: List<StorySeedEntry>
    ) {
        val unlockStateDao = database.unlockStateDao()
        val storySegmentDao = database.storySegmentDao()
        val milestoneDao = database.milestoneDao()

        val defaultsBySegmentId = mutableMapOf<Int, Pair<Boolean, Boolean>>()
        for (seed in seededStoryData) {
            val milestone = milestoneDao.getByDistanceMarker(seed.distanceMarker) ?: continue
            val segmentId = storySegmentDao.getByMilestoneId(milestone.id).firstOrNull()?.id ?: continue
            // Prevents an invalid default where read is true while unlocked is false.
            val normalizedReadDefault = if (seed.unlockedDefault) seed.readDefault else false
            defaultsBySegmentId[segmentId] = seed.unlockedDefault to normalizedReadDefault
        }

        val segments = storySegmentDao.getAll()
        if (segments.isEmpty()) return

        // Inserts only missing rows to preserve existing user progress.
        val missingStates = segments.mapNotNull { seg ->
            val existing = unlockStateDao.getByStorySegmentId(seg.id)
            if (existing != null) {
                null
            } else {
                val defaults = defaultsBySegmentId[seg.id] ?: (false to false)
                UnlockState(
                    storySegmentId = seg.id,
                    unlocked = defaults.first,
                    read = defaults.second
                )
            }
        }

        if (missingStates.isNotEmpty()) {
            unlockStateDao.upsertAll(missingStates)
        }
    }
}