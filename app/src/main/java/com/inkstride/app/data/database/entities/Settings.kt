package com.inkstride.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inkstride.app.data.DistanceUnit

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