package com.inkstride.app.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.inkstride.app.data.database.entities.Milestone
import com.inkstride.app.data.database.entities.Settings
import com.inkstride.app.data.database.entities.StorySegment
import com.inkstride.app.data.database.entities.UnlockState

object DatabaseProvider {
    @Volatile
    private var instance: InkstrideDatabase? = null

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

    suspend fun ensureDefaults(context: Context) {
        val database = getDatabase(context)
        ensureDefaultSettings(database)

        val characterName = database.settingsDao().get()?.normalized()?.characterName
            ?: Settings.DEFAULT_CHARACTER_NAME
        val seededStoryData = StorySeedDataSource.load(context, characterName)

        ensureSeededMilestonesAndStory(database, seededStoryData)
        ensureSeededUnlockStates(database, seededStoryData)
    }

    private suspend fun ensureDefaultSettings(database: InkstrideDatabase) {
        val settingsDao = database.settingsDao()
        val currentSettings = settingsDao.get()
        val normalizedSettings = (currentSettings ?: Settings()).normalized()

        if (currentSettings != normalizedSettings) {
            settingsDao.upsert(normalizedSettings)
        }
    }

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
            val normalizedReadDefault = if (seed.unlockedDefault) seed.readDefault else false
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