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

@Database(
    entities = [
        Settings::class,
        ProgressState::class,
        DailyStats::class,
        Milestone::class,
        StorySegment::class,
        UnlockState::class
    ],
    version = 7,
    exportSchema = true
)
abstract class InkstrideDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun progressStateDao(): ProgressStateDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun storySegmentDao(): StorySegmentDao
    abstract fun unlockStateDao(): UnlockStateDao
}