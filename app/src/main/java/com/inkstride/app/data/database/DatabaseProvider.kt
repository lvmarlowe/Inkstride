// app/src/main/java/com/inkstride/app/data/database/DatabaseProvider.kt
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

    // Holds the shared database reference so only one instance exists per process.
    @Volatile
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
                    DatabaseMigrator.MIGRATION_7_8,
                    DatabaseMigrator.MIGRATION_8_9
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

            for ((distanceMarker, isPersistent, isMajor, areaName, text, _, _, badgeColorRaw) in seededStoryData) {
                val normalizedAreaName = dataValidator.normalizeAreaNameForStorage(areaName)
                val badgeColor = dataValidator.normalizeBadgeColor(badgeColorRaw)
                val milestone = milestoneDao.getByDistanceMarker(distanceMarker)
                val milestoneId = milestone?.id
                    ?: milestoneDao.insert(
                        Milestone(
                            distanceMarker = distanceMarker,
                            isPersistent = isPersistent,
                            isMajor = isMajor,
                            areaName = normalizedAreaName,
                            badgeColor = badgeColor
                        )
                    ).toInt()

                if (
                    milestone != null && (
                            milestone.areaName != normalizedAreaName ||
                                    milestone.isMajor != isMajor ||
                                    milestone.isPersistent != isPersistent ||
                                    milestone.badgeColor != badgeColor
                            )
                ) {
                    milestoneDao.insert(
                        milestone.copy(
                            areaName = normalizedAreaName,
                            isMajor = isMajor,
                            isPersistent = isPersistent,
                            badgeColor = badgeColor
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
     * ensureSeededUnlockStates: Synchronizes unlock-state rows using seed defaults plus earned progress.
     * Preserves existing read progress for still-unlocked rows and normalizes read when locked.
     */
    private suspend fun ensureSeededUnlockStates(
        database: InkstrideDatabase,
        seededStoryData: List<StorySeedEntry>
    ) {
        val unlockStateDao = database.unlockStateDao()
        val storySegmentDao = database.storySegmentDao()
        val milestoneDao = database.milestoneDao()
        val currentDistanceMiles = database.progressStateDao().get()?.totalDistance ?: 0.0

        val defaultsBySegmentId = mutableMapOf<Int, Pair<Boolean, Boolean>>()
        val distanceBySegmentId = mutableMapOf<Int, Double>()
        for (seed in seededStoryData) {
            val milestone = milestoneDao.getByDistanceMarker(seed.distanceMarker) ?: continue
            val segmentId = storySegmentDao.getByMilestoneId(milestone.id).firstOrNull()?.id ?: continue
            val normalizedReadDefault = dataValidator.normalizeReadFlag(
                unlocked = seed.unlockedDefault,
                read = seed.readDefault
            )
            defaultsBySegmentId[segmentId] = seed.unlockedDefault to normalizedReadDefault
            distanceBySegmentId[segmentId] = seed.distanceMarker
        }

        val segments = storySegmentDao.getAll()
        if (segments.isEmpty()) return

        val desiredStates = mutableListOf<UnlockState>()

        for (seg in segments) {
            val defaults = defaultsBySegmentId[seg.id] ?: (false to false)
            val existing = unlockStateDao.getByStorySegmentId(seg.id)

            val milestoneDistance = distanceBySegmentId[seg.id]
            val unlockedByProgress = milestoneDistance != null && currentDistanceMiles >= milestoneDistance
            val desiredUnlocked = defaults.first || unlockedByProgress
            val desiredRead = dataValidator.normalizeReadFlag(
                unlocked = desiredUnlocked,
                read = if (desiredUnlocked) {
                    defaults.second || (existing?.read ?: false)
                } else {
                    false
                }
            )

            val desiredState = UnlockState(
                storySegmentId = seg.id,
                unlocked = desiredUnlocked,
                read = desiredRead
            )
            if (existing != desiredState) {
                desiredStates += desiredState
            }
        }

        if (desiredStates.isNotEmpty()) {
            unlockStateDao.upsertAll(desiredStates)
        }
    }
}