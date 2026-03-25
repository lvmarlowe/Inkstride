package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores unlock and read flags for each story segment.
 *
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

    // Uses segment id as the primary key for one-to-one state mapping.
    @PrimaryKey val storySegmentId: Int,

    // Marks segment as unlocked and available to read.
    val unlocked: Boolean = false,

    // Marks segment as already opened by the user.
    val read: Boolean = false
)