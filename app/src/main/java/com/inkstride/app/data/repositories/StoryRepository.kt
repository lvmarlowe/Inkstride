// app/src/main/java/com/inkstride/app/data/repositories/StoryRepository.kt
package com.inkstride.app.data.repositories

import android.content.Context
import com.inkstride.app.data.BadgeColor
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.database.StorySeedDataSource
import com.inkstride.app.data.database.entities.Settings
import com.inkstride.app.data.database.entities.StorySegment
import com.inkstride.app.services.DataValidator

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
    private val dataValidator = DataValidator()

    // Holds the intro segment id after first load so seed data is not re-read on every call.
    // Uses a placeholder value so a null result can be cached without a separate flag.
    private var introSegmentIdCache: Any? = INTRO_SEGMENT_ID_UNINITIALIZED

    /**
     * getIntroSegmentIfUnreadUnlocked: Returns the intro segment only when it is unlocked and unread.
     * Prevents first-run narrative content from replaying after the user has already read it.
     */
    suspend fun getIntroSegmentIfUnreadUnlocked(): StorySegment? {
        val introSegmentId = getIntroSegmentIdCached() ?: return null
        val introSegment = storySegmentDao.getById(introSegmentId) ?: return null
        val introState = unlockStateDao.getByStorySegmentId(introSegmentId) ?: return null
        return if (introState.unlocked && !introState.read) introSegment else null
    }

    /**
     * getStoryUnlockSegments: Returns unlock-ready story text and area labels for the requested ids.
     * Preserves caller order and fills missing values with empty strings so pager rendering stays stable.
     */
    suspend fun getStoryUnlockSegments(storySegmentIds: List<Int>): List<StoryUnlockSegment> {
        return storySegmentIds.map { segmentId ->
            val segment = storySegmentDao.getById(segmentId)
            StoryUnlockSegment(
                id = segmentId,
                text = segment?.text.orEmpty(),
                areaName = dataValidator.normalizeAreaNameForStorage(
                    milestoneDao.getAreaNameByStorySegmentId(segmentId)
                )
            )
        }
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

    // getUnlockedUnreadSegments: Returns unlocked, unread segments excluding the intro to avoid badge overlap.
    suspend fun getUnlockedUnreadSegments(): List<StorySegment> {
        val introId = getIntroSegmentIdCached()
        return storySegmentDao.getUnlockedUnreadOrderedByDistance()
            .filter { it.id != introId }
    }

    // hasUnlockedUnreadSegments: Returns true when at least one non-intro unlocked segment remains unread to drive inbox badge state.
    suspend fun hasUnlockedUnreadSegments(): Boolean {
        return getUnlockedUnreadSegments().isNotEmpty()
    }

    // getAreaNameForStorySegment: Returns milestone area name for a segment, or empty string when missing.
    suspend fun getAreaNameForStorySegment(storySegmentId: Int): String {
        return dataValidator.normalizeAreaNameForStorage(
            milestoneDao.getAreaNameByStorySegmentId(storySegmentId)
        )
    }

    // markAsRead: Marks a story segment as read to clear it from the story inbox.
    suspend fun markAsRead(storySegmentId: Int) {
        unlockStateDao.markAsRead(storySegmentId)
    }

    // getTotalDistance: Returns the current total distance from the progress state row, or zero when not initialized.
    suspend fun getTotalDistance(): Double {
        return database.progressStateDao().get()?.totalDistance ?: 0.0
    }

    // getStoryBadgeColor: Returns the badge color of the furthest unlocked milestone that has one set, or white when none applies.
    suspend fun getStoryBadgeColor(): BadgeColor {
        return dataValidator.normalizeBadgeColorEnum(
            milestoneDao.getBadgeColorForFurthestUnlocked()
        )
    }

    /**
     * getIntroSegmentIdCached: Returns a cached intro segment id when available, otherwise computes and stores it.
     * Caches null too so repeated calls do not keep re-reading seed data when intro content is missing.
     */
    private suspend fun getIntroSegmentIdCached(): Int? {
        if (introSegmentIdCache !== INTRO_SEGMENT_ID_UNINITIALIZED) {
            @Suppress("UNCHECKED_CAST")
            return introSegmentIdCache as Int?
        }

        introSegmentIdCache = loadIntroSegmentId()
        @Suppress("UNCHECKED_CAST")
        return introSegmentIdCache as Int?
    }

    // loadIntroSegmentId: Resolves the intro segment id from character seed data and milestone mapping.
    private suspend fun loadIntroSegmentId(): Int? {
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

// Placeholder object used to mark the intro segment id cache as not yet loaded.
private object IntroSegmentIdUninitialized

// Stores the placeholder instance for identity checks in getIntroSegmentIdCached.
private val INTRO_SEGMENT_ID_UNINITIALIZED: Any = IntroSegmentIdUninitialized

/**
 * StoryUnlockSegment: Stores unlock-screen text and area label for a specific story segment id.
 */
data class StoryUnlockSegment(
    val id: Int,
    val text: String,
    val areaName: String
)

/**
 * StorybookSegment: Stores storybook-ready segment text with an optional persistent area label.
 * Separates display-mapped data from raw entity fields to keep UI code clean.
 */
data class StorybookSegment(
    val text: String,
    val persistentAreaName: String?
)