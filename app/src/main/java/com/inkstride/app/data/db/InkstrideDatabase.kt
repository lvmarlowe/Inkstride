package com.inkstride.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.inkstride.app.data.db.dao.DailyStatsDao
import com.inkstride.app.data.db.dao.MilestoneDao
import com.inkstride.app.data.db.dao.ProgressStateDao
import com.inkstride.app.data.db.dao.SettingsDao
import com.inkstride.app.data.db.dao.StorySegmentDao
import com.inkstride.app.data.db.dao.UnlockStateDao
import com.inkstride.app.data.db.entities.DailyStats
import com.inkstride.app.data.db.entities.Milestone
import com.inkstride.app.data.db.entities.ProgressState
import com.inkstride.app.data.db.entities.Settings
import com.inkstride.app.data.db.entities.StorySegment
import com.inkstride.app.data.db.entities.UnlockState

@Database(
    entities = [
        Settings::class,
        ProgressState::class,
        DailyStats::class,
        Milestone::class,
        StorySegment::class,
        UnlockState::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InkstrideDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun progressStateDao(): ProgressStateDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun storySegmentDao(): StorySegmentDao
    abstract fun unlockStateDao(): UnlockStateDao
}