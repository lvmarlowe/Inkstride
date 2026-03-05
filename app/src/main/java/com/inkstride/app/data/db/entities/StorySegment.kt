package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "story_segment",
    foreignKeys = [
        ForeignKey(
            entity = Milestone::class,
            parentColumns = ["id"],
            childColumns = ["milestoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["milestoneId"], unique = true)
    ]
)
data class StorySegment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val milestoneId: Int,
    val text: String
)