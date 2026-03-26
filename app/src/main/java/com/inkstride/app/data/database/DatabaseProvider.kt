package com.inkstride.app.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.inkstride.app.data.database.entities.Milestone
import com.inkstride.app.data.database.entities.Settings
import com.inkstride.app.data.database.entities.StorySegment
import com.inkstride.app.data.database.entities.UnlockState
import com.inkstride.app.services.DataValidator

/**
 * DatabaseProvider: Provides database access and default seed setup.
 * Keeps startup data rules in one place so first launch and upgrades behave the same way.
 */
object DatabaseProvider {

    @Volatile
    // Holds the shared database reference so only one instance exists per process.
    private var instance: InkstrideDatabase? = null

    // Applies shared normalization rules during seed and default setup.
    private val dataValidator = DataValidator()

    /**
     * getDatabase: Returns the shared Room database instance.
     * Builds once with required migrations and reuses the same reference afterward.
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
     * ensureDefaults: Ensures default settings and seed data exist on startup.
     * Normalizes values before writing so runtime behavior matches database constraints.
     */
    suspend fun ensureDefaults(context: Context) {
        val database = getDatabase(context)
        ensureDefaultSettings(database)

        val characterName = database.settingsDao().get()?.normalized()?.characterName
            ?: DataValidator.DEFAULT_CHARACTER_NAME
        val seededStoryData = StorySeedDataSource.load(context, characterName)

        ensureSeededMilestonesAndStory(database, seededStoryData)
        ensureSeededUnlockStates(database, seededStoryData)
    }

    /**
     * ensureDefaultSettings: Ensures the settings table has one normalized row.
     * Writes only when the current row differs from its normalized form to avoid unnecessary updates.
     */
    private suspend fun ensureDefaultSettings(database: InkstrideDatabase) {
        val settingsDao = database.settingsDao()
        val currentSettings = settingsDao.get()
        val normalizedSettings = (currentSettings ?: Settings()).normalized()

        if (currentSettings != normalizedSettings) {
            settingsDao.upsert(normalizedSettings)
        }
    }

    /**
     * ensureSeededMilestonesAndStory: Ensures milestone and story segment rows match the bundled seed set.
     * Inserts or updates existing rows to keep text and metadata aligned with the latest seed file.
     */
    private suspend fun ensureSeededMilestonesAndStory(
        database: InkstrideDatabase,
        seededStoryData: List<StorySeedEntry>
    ) {
        database.withTransaction {
            val milestoneDao = database.milestoneDao()
            val storySegmentDao = database.storySegmentDao()

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
                    storySegmentDao.insert(existingSegment.copy(text = text))
                }
            }
        }
    }

    /**
     * ensureSeededUnlockStates: Ensures each story segment has an unlock state row.
     * Normalizes read defaults so a segment cannot be marked read before it is unlocked.
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
            val normalizedReadDefault = dataValidator.normalizeReadFlag(
                unlocked = seed.unlockedDefault,
                read = seed.readDefault
            )
            defaultsBySegmentId[segmentId] = seed.unlockedDefault to normalizedReadDefault
        }

        val segments = storySegmentDao.getAll()
        if (segments.isEmpty()) return

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