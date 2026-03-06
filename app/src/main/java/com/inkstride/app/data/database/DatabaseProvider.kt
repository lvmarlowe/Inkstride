package com.inkstride.app.data.database

import android.content.Context
import androidx.room.Room
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
                .fallbackToDestructiveMigration(true)
                .build()
            instance = newInstance
            newInstance
        }
    }

    suspend fun ensureDefaults(context: Context) {
        val database = getDatabase(context)
        ensureDefaultSettings(database)
        ensureSeededMilestonesAndStory(database)
        ensureSeededUnlockStates(database)
    }

    private suspend fun ensureDefaultSettings(database: InkstrideDatabase) {
        val settingsDao = database.settingsDao()
        val currentSettings = settingsDao.get()
        val normalizedSettings = (currentSettings ?: Settings()).normalized()

        if (currentSettings != normalizedSettings) {
            settingsDao.upsert(normalizedSettings)
        }
    }

    private suspend fun ensureSeededMilestonesAndStory(database: InkstrideDatabase) {
        val milestoneDao = database.milestoneDao()
        val storySegmentDao = database.storySegmentDao()

        val characterName = database.settingsDao().get()?.normalized()?.characterName
            ?: Settings.DEFAULT_CHARACTER_NAME

        val seededStoryData = listOf(
            Triple(0.0, true, "Hello, $characterName!"),
            Triple(6.0, true, "[main story segment 1]"),
            Triple(13.0, true, "[main story segment 2]"),
            Triple(22.0, true, "[main story segment 3]"),
            Triple(34.0, true, "[main story segment 4]"),
            Triple(50.0, true, "[main story segment 5]"),
            Triple(71.0, true, "[main story segment 6]"),
            Triple(100.0, true, "[act 1 complete]")
        )

        for ((distanceMarker, isMajor, text) in seededStoryData) {
            val milestone = milestoneDao.getByDistanceMarker(distanceMarker)
            val milestoneId = milestone?.id
                ?: milestoneDao.insert(
                    Milestone(
                        distanceMarker = distanceMarker,
                        isMajor = isMajor
                    )
                ).toInt()

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

    private suspend fun ensureSeededUnlockStates(database: InkstrideDatabase) {
        val unlockStateDao = database.unlockStateDao()
        val storySegmentDao = database.storySegmentDao()

        val segments = storySegmentDao.getAll()
        if (segments.isEmpty()) return

        val missingStates = segments.mapNotNull { seg ->
            val existing = unlockStateDao.getByStorySegmentId(seg.id)
            if (existing == null) {
                UnlockState(
                    storySegmentId = seg.id,
                    unlocked = false,
                    read = false
                )
            } else {
                null
            }
        }

        if (missingStates.isNotEmpty()) {
            unlockStateDao.upsertAll(missingStates)
        }
    }
}