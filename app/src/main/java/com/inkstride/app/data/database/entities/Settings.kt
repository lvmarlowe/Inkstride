package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inkstride.app.data.DistanceUnit

/**
 * Stores user settings for the active profile.
 *
 * Table uses a fixed primary key value so settings live in one row.
 */
@Entity(tableName = "settings")
data class Settings(

    // Fixed single row id.
    @PrimaryKey val id: Int = 1,

    // Stores display name used in story and UI copy.
    val characterName: String = DEFAULT_CHARACTER_NAME,

    // Stores selected distance unit using storage-safe enum value.
    val distanceUnit: String = DistanceUnit.MILE.storageValue
) {
    /**
     * Returns a normalized settings copy with safe persisted values.
     *
     * Name value trims whitespace and falls back to default when blank.
     * Unit value maps to a known DistanceUnit storage value.
     */
    fun normalized(): Settings {
        val normalizedName = characterName.trim().ifBlank { DEFAULT_CHARACTER_NAME }
        val normalizedUnit = DistanceUnit.fromStorageValue(distanceUnit).storageValue
        return copy(characterName = normalizedName, distanceUnit = normalizedUnit)
    }

    companion object {
        const val DEFAULT_CHARACTER_NAME = "Inker"
    }
}