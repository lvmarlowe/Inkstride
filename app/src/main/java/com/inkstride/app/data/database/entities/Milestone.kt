package com.inkstride.app.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores route milestones used for story unlock and progression logic.
 *
 * Distance markers stay unique so each route point maps
 * to one milestone row.
 */
@Entity(
    tableName = "milestone",
    indices = [Index(value = ["distanceMarker"], unique = true)]
)
data class Milestone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Stores route distance in the app's base distance unit.
    val distanceMarker: Double,

    // Flags major milestones for prominent UI treatment.
    val isMajor: Boolean = false,
    @ColumnInfo(name = "area_name")

    // Stores the user-facing location label for the milestone.
    val areaName: String = "",
    @ColumnInfo(name = "is_persistent")

    // Keeps milestone available across resets when set to true.
    val isPersistent: Boolean = true
)