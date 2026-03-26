package com.inkstride.app.services

import android.content.Context
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.database.entities.UnlockState

/**
 * MilestoneEngine: Checks reached milestones and unlocks their story segments.
 * Keeps unlock logic in one place so distance-based progression stays consistent across sync calls.
 */
class MilestoneEngine(context: Context) {

    // Shares one database handle across all milestone and unlock operations.
    private val database = DatabaseProvider.getDatabase(context)
    private val milestoneDao = database.milestoneDao()
    private val storySegmentDao = database.storySegmentDao()
    private val unlockStateDao = database.unlockStateDao()

    /**
     * checkAndUnlockForDistance: Unlocks story segments for all milestones at or below current distance.
     * Skips segments that are already unlocked to avoid overwriting existing read state.
     */
    suspend fun checkAndUnlockForDistance(currentDistance: Double) {
        val milestones = milestoneDao.getAll()
        if (milestones.isEmpty()) return

        val reached = milestones.filter { it.distanceMarker <= currentDistance }
        if (reached.isEmpty()) return

        val toUpsert = mutableListOf<UnlockState>()
        for (m in reached) {
            val segments = storySegmentDao.getByMilestoneId(m.id)
            for (s in segments) {
                val existing = unlockStateDao.getByStorySegmentId(s.id)
                if (existing == null) {
                    toUpsert.add(UnlockState(storySegmentId = s.id, unlocked = true, read = false))
                } else if (!existing.unlocked) {
                    toUpsert.add(existing.copy(unlocked = true, read = false))
                }
            }
        }

        if (toUpsert.isNotEmpty()) {
            unlockStateDao.upsertAll(toUpsert)
        }
    }
}