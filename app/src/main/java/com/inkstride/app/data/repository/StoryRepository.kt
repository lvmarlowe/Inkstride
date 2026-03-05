package com.inkstride.app.data.repository

import android.content.Context
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.db.entities.StorySegment

class StoryRepository(context: Context) {

    private val database = DatabaseProvider.getDatabase(context)
    private val storySegmentDao = database.storySegmentDao()
    private val unlockStateDao = database.unlockStateDao()

    suspend fun getIntroSegmentIfUnreadUnlocked(): StorySegment? {
        val milestones = database.milestoneDao().getAll()
        if (milestones.isEmpty()) return null

        val introMilestone = milestones.firstOrNull { it.distanceMarker <= 0.0 } ?: milestones.first()
        val introSegment = storySegmentDao.getByMilestoneId(introMilestone.id).firstOrNull() ?: return null
        val introState = unlockStateDao.getByStorySegmentId(introSegment.id) ?: return null

        return if (introState.unlocked && !introState.read) introSegment else null
    }

    suspend fun getReadUnlockedSegments(): List<StorySegment> {
        return storySegmentDao.getReadUnlockedOrderedByDistance()
    }

    suspend fun getUnlockedUnreadSegments(): List<StorySegment> {
        return storySegmentDao.getUnlockedUnreadOrderedByDistance()
    }

    suspend fun hasUnlockedUnreadSegments(): Boolean {
        return unlockStateDao.hasAnyUnlockedUnread()
    }

    suspend fun markAsRead(storySegmentId: Int) {
        unlockStateDao.markAsRead(storySegmentId)
    }
}