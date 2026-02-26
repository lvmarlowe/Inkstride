package com.inkstride.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val characterName: String = DEFAULT_CHARACTER_NAME,
    val distanceUnit: String = DistanceUnit.MILE.storageValue
) {
    fun normalized(): Settings {
        val normalizedName = characterName.trim().ifBlank { DEFAULT_CHARACTER_NAME }
        val normalizedUnit = DistanceUnit.fromStorageValue(distanceUnit).storageValue
        return copy(characterName = normalizedName, distanceUnit = normalizedUnit)
    }

    companion object {
        const val DEFAULT_CHARACTER_NAME = "Inker"
    }
}

enum class DistanceUnit(val storageValue: String) {
    MILE(storageValue = "mile"),
    KILOMETER(storageValue = "kilometer");

    companion object {
        fun fromStorageValue(value: String?): DistanceUnit {
            return entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: MILE
        }
    }
}
