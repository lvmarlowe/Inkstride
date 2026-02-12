package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_segment")
data class StorySegment(
    @PrimaryKey val id: Int,
    val milestoneId: Int,
    val text: String
)
