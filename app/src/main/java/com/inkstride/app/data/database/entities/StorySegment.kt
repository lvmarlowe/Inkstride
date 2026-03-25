package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores one story segment tied to one milestone.
 *
 * Unique milestone index enforces one segment per milestone,
 * which keeps unlock progression deterministic.
 */
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

    // Links segment to milestone parent row.
    val milestoneId: Int,

    // Stores full narrative content shown to the user.
    val text: String
)