package com.inkstride.app.data

/**
 * DistanceUnit: Defines supported distance units with their database storage values.
 * Centralizes unit identity so string comparisons stay consistent across the app.
 */
enum class DistanceUnit(val storageValue: String) {
    MILE(storageValue = "mile"),
    KILOMETER(storageValue = "kilometer");

    companion object {
        // fromStorageValue: Returns the matching DistanceUnit, or MILE when the value is unknown or null.
        fun fromStorageValue(value: String?): DistanceUnit {
            return entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: MILE
        }
    }
}