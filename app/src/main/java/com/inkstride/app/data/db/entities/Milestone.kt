package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestone")
data class Milestone(
    @PrimaryKey val id: Int,
    val mileMarker: Double,
    val isMajor: Boolean = false
)
