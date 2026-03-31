package com.inkstride.app.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Milestone: Stores route milestones used for story unlock and progression logic.
 * Distance markers stay unique so each route point maps to one milestone row.
 */
@Entity(
    tableName = "milestone",
    indices = [Index(value = ["distanceMarker"], unique = true)]
)
data class Milestone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Stores route distance in the app's base distance unit to mark where story unlocks trigger.
    val distanceMarker: Double,

    // Flags major milestones for prominent UI treatment to distinguish key story moments.
    val isMajor: Boolean = false,

    @ColumnInfo(name = "area_name")
    // Stores the user-facing location label displayed in story and recap views.
    val areaName: String = "",

    @ColumnInfo(name = "is_persistent")
    // Keeps milestone available across resets so anchor story points are never lost.
    val isPersistent: Boolean = true,

    @ColumnInfo(name = "badge_color")
    // Stores the badge color transition triggered at this milestone, or empty string when none.
    val badgeColor: String = ""
)