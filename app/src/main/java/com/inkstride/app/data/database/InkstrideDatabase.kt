package com.inkstride.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.inkstride.app.data.database.daos.DailyStatsDao
import com.inkstride.app.data.database.daos.MilestoneDao
import com.inkstride.app.data.database.daos.ProgressStateDao
import com.inkstride.app.data.database.daos.SettingsDao
import com.inkstride.app.data.database.daos.StorySegmentDao
import com.inkstride.app.data.database.daos.UnlockStateDao
import com.inkstride.app.data.database.entities.DailyStats
import com.inkstride.app.data.database.entities.Milestone
import com.inkstride.app.data.database.entities.ProgressState
import com.inkstride.app.data.database.entities.Settings
import com.inkstride.app.data.database.entities.StorySegment
import com.inkstride.app.data.database.entities.UnlockState

/**
 * Defines the Room database and DAO entry points.
 */
@Database(
    entities = [
        Settings::class,
        ProgressState::class,
        DailyStats::class,
        Milestone::class,
        StorySegment::class,
        UnlockState::class
    ],
    version = 8,
    exportSchema = true
)
abstract class InkstrideDatabase : RoomDatabase() {

    /**
     * Returns DAO for settings rows.
     */
    abstract fun settingsDao(): SettingsDao

    /**
     * Returns DAO for progress-state rows.
     */
    abstract fun progressStateDao(): ProgressStateDao

    /**
     * Returns DAO for daily stats rows.
     */
    abstract fun dailyStatsDao(): DailyStatsDao

    /**
     * Returns DAO for milestone rows.
     */
    abstract fun milestoneDao(): MilestoneDao

    /**
     * Returns DAO for story segment rows.
     */
    abstract fun storySegmentDao(): StorySegmentDao

    /**
     * Returns DAO for unlock-state rows.
     */
    abstract fun unlockStateDao(): UnlockStateDao
}