package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * UnlockState: Stores unlock and read flags for each story segment.
 * Composite index supports fast queries for unread badge checks
 * and ordered inbox/recap retrieval.
 */
@Entity(
    tableName = "unlock_state",
    foreignKeys = [
        ForeignKey(
            entity = StorySegment::class,
            parentColumns = ["id"],
            childColumns = ["storySegmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["storySegmentId"], unique = true),
        Index(value = ["unlocked", "read", "storySegmentId"])
    ]
)
data class UnlockState(

    // Uses segment id as the primary key to enforce one-to-one state mapping per segment.
    @PrimaryKey val storySegmentId: Int,

    // Marks segment as unlocked and available to read when the user reaches the milestone.
    val unlocked: Boolean = false,

    // Marks segment as already opened by the user to clear it from the story inbox.
    val read: Boolean = false
)