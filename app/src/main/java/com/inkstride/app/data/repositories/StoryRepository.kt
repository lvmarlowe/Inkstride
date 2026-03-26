package com.inkstride.app.data.repositories

import android.content.Context
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.database.entities.StorySegment
import com.inkstride.app.services.DataValidator

/**
 * StoryRepository: Handles story reads and state updates used by story and storybook screens.
 * Keeps DAO calls together so UI code stays focused on rendering.
 */
class StoryRepository(context: Context) {

    // Shares one database handle across repository methods.
    private val database = DatabaseProvider.getDatabase(context)

    // Reads and maps story segment rows.
    private val storySegmentDao = database.storySegmentDao()

    // Reads and updates unlock and read state rows.
    private val unlockStateDao = database.unlockStateDao()

    // Reads milestone metadata used for story grouping.
    private val milestoneDao = database.milestoneDao()

    // Applies shared cleanup rules for mapped storybook values.
    private val dataValidator = DataValidator()

    /**
     * getIntroSegmentIfUnreadUnlocked: Returns the intro segment only when it is unlocked and unread.
     * Prevents first-run narrative content from replaying after the user has already read it.
     */
    suspend fun getIntroSegmentIfUnreadUnlocked(): StorySegment? {
        val milestones = database.milestoneDao().getAll()
        if (milestones.isEmpty()) return null

        val introMilestone = milestones.firstOrNull { it.distanceMarker <= 0.0 } ?: milestones.first()
        val introSegment = storySegmentDao.getByMilestoneId(introMilestone.id).firstOrNull() ?: return null
        val introState = unlockStateDao.getByStorySegmentId(introSegment.id) ?: return null

        return if (introState.unlocked && !introState.read) introSegment else null
    }

    // getReadUnlockedSegments: Returns read and unlocked story segments in distance order for recap display.
    suspend fun getReadUnlockedSegments(): List<StorySegment> {
        return storySegmentDao.getReadUnlockedOrderedByDistance()
    }

    /**
     * getReadUnlockedStorybookSegments: Returns mapped storybook items with optional persistent area names.
     * Trims area names and converts blank values to null for cleaner display handling.
     */
    suspend fun getReadUnlockedStorybookSegments(): List<StorybookSegment> {
        return storySegmentDao.getReadUnlockedOrderedByDistance().map { segment ->
            val milestone = milestoneDao.getById(segment.milestoneId)
            StorybookSegment(
                text = segment.text,
                persistentAreaName = if (milestone?.isPersistent == true) {
                    dataValidator.normalizeAreaName(milestone.areaName)
                } else {
                    null
                }
            )
        }
    }

    // getUnlockedUnreadSegments: Returns unlocked segments that are still unread for story inbox display.
    suspend fun getUnlockedUnreadSegments(): List<StorySegment> {
        return storySegmentDao.getUnlockedUnreadOrderedByDistance()
    }

    // hasUnlockedUnreadSegments: Returns true when at least one unlocked segment remains unread to drive inbox badge state.
    suspend fun hasUnlockedUnreadSegments(): Boolean {
        return unlockStateDao.hasAnyUnlockedUnread()
    }

    // getAreaNameForStorySegment: Returns milestone area name for a segment, or empty string when missing.
    suspend fun getAreaNameForStorySegment(storySegmentId: Int): String {
        return milestoneDao.getAreaNameByStorySegmentId(storySegmentId).orEmpty()
    }

    // markAsRead: Marks a story segment as read to clear it from the story inbox.
    suspend fun markAsRead(storySegmentId: Int) {
        unlockStateDao.markAsRead(storySegmentId)
    }
}

/**
 * StorybookSegment: Stores storybook-ready segment text with an optional persistent area label.
 * Separates display-mapped data from raw entity fields to keep UI code clean.
 */
data class StorybookSegment(
    val text: String,
    val persistentAreaName: String?
)