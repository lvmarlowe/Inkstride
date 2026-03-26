package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inkstride.app.services.DataValidator

/**
 * Settings: Stores user settings for the active profile.
 * Table uses a fixed primary key value so settings live in one row.
 */
@Entity(tableName = "settings")
data class Settings(

    // Fixed row id keeps settings contained to one row per profile.
    @PrimaryKey val id: Int = 1,

    // Stores display name used in story and UI text to personalize the experience.
    val characterName: String = DEFAULT_CHARACTER_NAME,

    // Stores selected distance unit as a safe enum storage value for consistent conversions.
    val distanceUnit: String = "mile"
) {
    /**
     * normalized: Returns a settings copy with safe persisted values.
     * Name trims whitespace and falls back to default when blank.
     * Unit maps to a known DistanceUnit storage value to prevent invalid entries.
     */
    fun normalized(): Settings {
        val dataValidator = DataValidator()
        val normalizedName = dataValidator.normalizeCharacterName(characterName)
        val normalizedUnit = dataValidator.normalizeDistanceUnit(distanceUnit)
        return copy(characterName = normalizedName, distanceUnit = normalizedUnit)
    }

    companion object {
        // Mirrors the default from DataValidator to keep character name consistent across files.
        const val DEFAULT_CHARACTER_NAME = DataValidator.DEFAULT_CHARACTER_NAME
    }
}