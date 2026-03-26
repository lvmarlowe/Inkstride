package com.inkstride.app.data.repositories

import android.content.Context
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.database.StorySeedDataSource
import com.inkstride.app.data.database.entities.Settings
import com.inkstride.app.data.database.entities.StorySegment

/**
 * StoryRepository: Handles story reads and state updates used by story and storybook screens.
 * Keeps DAO calls together so UI code stays focused on rendering.
 */
class StoryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val database = DatabaseProvider.getDatabase(appContext)
    private val storySegmentDao = database.storySegmentDao()
    private val unlockStateDao = database.unlockStateDao()
    private val milestoneDao = database.milestoneDao()

    /**
     * getIntroSegmentIfUnreadUnlocked: Returns the intro segment only when it is unlocked and unread.
     * Prevents first-run narrative content from replaying after the user has already read it.
     */
    suspend fun getIntroSegmentIfUnreadUnlocked(): StorySegment? {
        val characterName = database.settingsDao().get()?.normalized()?.characterName
            ?: Settings.DEFAULT_CHARACTER_NAME
        val introDistanceMarker = StorySeedDataSource
            .load(appContext, characterName)
            .firstOrNull { it.unlockedDefault }
            ?.distanceMarker
            ?: return null
        val introMilestone = milestoneDao.getByDistanceMarker(introDistanceMarker) ?: return null
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
                    milestone.areaName.trim().ifBlank { null }
                } else {
                    null
                }
            )
        }
    }

    // getUnlockedUnreadSegments: Returns unlocked, unread segments excluding the intro to avoid badge overlap.
    suspend fun getUnlockedUnreadSegments(): List<StorySegment> {
        val introId = getIntroSegmentId()
        return storySegmentDao.getUnlockedUnreadOrderedByDistance()
            .filter { it.id != introId }
    }

    // hasUnlockedUnreadSegments: Returns true when at least one non-intro unlocked segment remains unread to drive inbox badge state.
    suspend fun hasUnlockedUnreadSegments(): Boolean {
        val introId = getIntroSegmentId()
        return storySegmentDao.getUnlockedUnreadOrderedByDistance()
            .any { it.id != introId }
    }

    // getAreaNameForStorySegment: Returns milestone area name for a segment, or empty string when missing.
    suspend fun getAreaNameForStorySegment(storySegmentId: Int): String {
        return milestoneDao.getAreaNameByStorySegmentId(storySegmentId).orEmpty()
    }

    // markAsRead: Marks a story segment as read to clear it from the story inbox.
    suspend fun markAsRead(storySegmentId: Int) {
        unlockStateDao.markAsRead(storySegmentId)
    }

    // getIntroSegmentId: Returns the segment id for the first seed entry with unlockedDefault true, or null if not found.
    private suspend fun getIntroSegmentId(): Int? {
        val characterName = database.settingsDao().get()?.normalized()?.characterName
            ?: Settings.DEFAULT_CHARACTER_NAME
        val introDistanceMarker = StorySeedDataSource
            .load(appContext, characterName)
            .firstOrNull { it.unlockedDefault }
            ?.distanceMarker
            ?: return null
        val introMilestone = milestoneDao.getByDistanceMarker(introDistanceMarker) ?: return null
        return storySegmentDao.getByMilestoneId(introMilestone.id).firstOrNull()?.id
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