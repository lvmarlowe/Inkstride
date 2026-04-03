package com.inkstride.app.services

import com.inkstride.app.data.DistanceUnit
import com.inkstride.app.data.BadgeColor

/**
 * DataValidator: Centralizes data cleanup rules used across repositories and services.
 * Keeps rules in one place so behavior is consistent before data reaches the database.
 */
class DataValidator {

    // normalizeCharacterName: Trims extra spaces, limits length, and returns the default name when blank.
    fun normalizeCharacterName(raw: String): String {
        return raw.trim().take(40).ifBlank { DEFAULT_CHARACTER_NAME }
    }

    // normalizeDistanceUnit: Maps to a known DistanceUnit so unknown or null values use the default.
    fun normalizeDistanceUnit(raw: String?): String {
        return DistanceUnit.fromStorageValue(raw).storageValue
    }

    // coerceDistance: Floors the value at zero to match persistence rules and UI expectations.
    fun coerceDistance(value: Double): Double {
        return value.coerceAtLeast(0.0)
    }

    // coerceSteps: Floors the value at zero to prevent invalid totals from propagating.
    fun coerceSteps(value: Long): Long {
        return value.coerceAtLeast(0L)
    }

    // coerceDayNumber: Floors the value at one so journey progress never reports day zero.
    fun coerceDayNumber(value: Int): Int {
        return value.coerceAtLeast(1)
    }

    // normalizeReadFlag: Forces read to false when the item is not unlocked.
    fun normalizeReadFlag(unlocked: Boolean, read: Boolean): Boolean {
        return unlocked && read
    }

    // normalizeAreaName: Trims whitespace from a nullable string and returns null when blank.
    fun normalizeAreaName(raw: String?): String? {
        return raw?.trim().orEmpty().ifBlank { null }
    }

    // normalizeAreaNameForStorage: Returns a trimmed area name or empty string for database storage fields that cannot be null.
    fun normalizeAreaNameForStorage(raw: String?): String {
        return normalizeAreaName(raw).orEmpty()
    }

    // normalizeBadgeColor: Accepts supported badge values (WHITE/GOLD) and returns blank for unknown or absent values.
    fun normalizeBadgeColor(raw: String?): String {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isBlank()) return ""

        return BadgeColor.entries.firstOrNull {
            it.name.equals(candidate, ignoreCase = true)
        }?.name ?: ""
    }

    // normalizeBadgeColorEnum: Maps a raw string to a BadgeColor enum value, defaulting to WHITE when unknown or absent.
    fun normalizeBadgeColorEnum(raw: String?): BadgeColor {
        val normalized = normalizeBadgeColor(raw)
        return BadgeColor.entries.firstOrNull { it.name == normalized } ?: BadgeColor.WHITE
    }

    companion object {
        // Default character name used when user input is blank.
        const val DEFAULT_CHARACTER_NAME = "Inker"
    }
}