package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_state")
data class UnlockState(
    @PrimaryKey val storySegmentId: Int,
    val unlocked: Boolean = false,
    val read: Boolean = false
)
