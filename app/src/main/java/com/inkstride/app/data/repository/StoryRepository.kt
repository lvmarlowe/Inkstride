package com.inkstride.app.data.repository

import android.content.Context
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.db.entities.StorySegment

class StoryRepository(context: Context) {

    private val database = DatabaseProvider.getDatabase(context)
    private val milestoneDao = database.milestoneDao()
    private val storySegmentDao = database.storySegmentDao()
    private val unlockStateDao = database.unlockStateDao()

    suspend fun getIntroSegmentIfUnreadUnlocked(): StorySegment? {
        val milestones = milestoneDao.getAll()
        if (milestones.isEmpty()) return null
        val introMilestone = milestones.firstOrNull { it.distanceMarker <= 0.0 } ?: milestones.first()
        val segments = storySegmentDao.getByMilestoneId(introMilestone.id)
        val introSegment = segments.firstOrNull() ?: return null
        val state = unlockStateDao.getByStorySegmentId(introSegment.id) ?: return null
        return if (state.unlocked && !state.read) introSegment else null
    }

    suspend fun markAsRead(storySegmentId: Int) {
        unlockStateDao.markAsRead(storySegmentId)
    }
}