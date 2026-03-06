package com.inkstride.app.data

enum class DistanceUnit(val storageValue: String) {
    MILE(storageValue = "mile"),
    KILOMETER(storageValue = "kilometer");

    companion object {
        fun fromStorageValue(value: String?): DistanceUnit {
            return entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: MILE
        }
    }
}