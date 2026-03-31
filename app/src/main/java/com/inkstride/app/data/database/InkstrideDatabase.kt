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
 * InkstrideDatabase: Defines the Room database and DAO entry points.
 * Registers all entities and exposes access methods for each table.
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
    version = 9,
    exportSchema = true
)
abstract class InkstrideDatabase : RoomDatabase() {

    // settingsDao: Returns DAO for settings rows.
    abstract fun settingsDao(): SettingsDao

    // progressStateDao: Returns DAO for progress state rows.
    abstract fun progressStateDao(): ProgressStateDao

    // dailyStatsDao: Returns DAO for daily stats rows.
    abstract fun dailyStatsDao(): DailyStatsDao

    // milestoneDao: Returns DAO for milestone rows.
    abstract fun milestoneDao(): MilestoneDao

    // storySegmentDao: Returns DAO for story segment rows.
    abstract fun storySegmentDao(): StorySegmentDao

    // unlockStateDao: Returns DAO for unlock state rows.
    abstract fun unlockStateDao(): UnlockStateDao
}