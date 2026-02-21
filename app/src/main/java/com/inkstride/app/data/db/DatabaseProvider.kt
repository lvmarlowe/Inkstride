package com.inkstride.app.data.db

import android.content.Context
import androidx.room.Room
import com.inkstride.app.data.db.entities.Milestone
import com.inkstride.app.data.db.entities.Settings
import com.inkstride.app.data.db.entities.StorySegment
import com.inkstride.app.data.db.entities.UnlockState

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
        ensureDefaultSettings(context)
        ensureSeededMilestonesAndStory(context)
        ensureSeededUnlockStates(context)
    }

    private suspend fun ensureDefaultSettings(context: Context) {
        val database = getDatabase(context)
        val settingsDao = database.settingsDao()

        if (settingsDao.countRows() == 0) {
            settingsDao.upsert(Settings())
        }
    }

    private suspend fun ensureSeededMilestonesAndStory(context: Context) {
        val database = getDatabase(context)
        val milestoneDao = database.milestoneDao()
        val storySegmentDao = database.storySegmentDao()

        if (milestoneDao.countRows() > 0 && storySegmentDao.countRows() > 0) return

        val milestones = listOf(
            Milestone(id = 1, distanceMarker = 6.0, isMajor = true),
            Milestone(id = 2, distanceMarker = 13.0, isMajor = true),
            Milestone(id = 3, distanceMarker = 22.0, isMajor = true),
            Milestone(id = 4, distanceMarker = 34.0, isMajor = true),
            Milestone(id = 5, distanceMarker = 50.0, isMajor = true),
            Milestone(id = 6, distanceMarker = 71.0, isMajor = true),
            Milestone(id = 7, distanceMarker = 100.0, isMajor = true),
            Milestone(id = 8, distanceMarker = 0.0, isMajor = true)
        )

        val segments = listOf(
            StorySegment(id = 1, milestoneId = 1, text = "[main story segment 1]"),
            StorySegment(id = 2, milestoneId = 2, text = "[main story segment 2]"),
            StorySegment(id = 3, milestoneId = 3, text = "[main story segment 3]"),
            StorySegment(id = 4, milestoneId = 4, text = "[main story segment 4]"),
            StorySegment(id = 5, milestoneId = 5, text = "[main story segment 5]"),
            StorySegment(id = 6, milestoneId = 6, text = "[main story segment 6]"),
            StorySegment(id = 7, milestoneId = 7, text = "[act 1 complete]"),
            StorySegment(id = 8, milestoneId = 8, text = "[act 1 intro]")
        )

        if (milestoneDao.countRows() == 0) milestoneDao.insertAll(milestones)
        if (storySegmentDao.countRows() == 0) storySegmentDao.insertAll(segments)
    }

    private suspend fun ensureSeededUnlockStates(context: Context) {
        val database = getDatabase(context)
        val unlockStateDao = database.unlockStateDao()
        val storySegmentDao = database.storySegmentDao()

        if (unlockStateDao.countRows() > 0) return

        val segments = storySegmentDao.getAll()
        if (segments.isEmpty()) return

        val states = segments.map { seg ->
            UnlockState(
                storySegmentId = seg.id,
                unlocked = false,
                read = false
            )
        }

        unlockStateDao.upsertAll(states)
    }
}