package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @PrimaryKey val storySegmentId: Int,
    val unlocked: Boolean = false,
    val read: Boolean = false
)