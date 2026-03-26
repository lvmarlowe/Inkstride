package com.inkstride.app.services

import com.inkstride.app.data.DistanceUnit

/**
 * DataValidator: Centralizes data cleanup rules used across repositories and services.
 * Keeps rules in one place so behavior is consistent before data reaches the database.
 */
class DataValidator {

    // normalizeCharacterName: Trims extra spaces and returns the default name when blank.
    fun normalizeCharacterName(raw: String): String {
        return raw.trim().ifBlank { DEFAULT_CHARACTER_NAME }
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
        return if (unlocked) read else false
    }

    // normalizeAreaName: Trims whitespace and returns null when the name is blank.
    fun normalizeAreaName(raw: String): String? {
        return raw.trim().ifBlank { null }
    }

    companion object {
        // Default character name used when user input is blank.
        const val DEFAULT_CHARACTER_NAME = "Inker"
    }
}