package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "milestone",
    indices = [Index(value = ["distanceMarker"], unique = true)]
)
data class Milestone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val distanceMarker: Double,
    val isMajor: Boolean = false
)