package com.inkstride.app.data.database

import android.content.Context
import org.json.JSONArray

/**
 * Loads seeded story rows from bundled JSON.
 */
object StorySeedDataSource {

    // Points to the bundled seed-data asset file.
    private const val STORY_SEED_ASSET_PATH = "story_seed_data.json"

    // Marks the token replaced with the player-selected character name.
    private const val CHARACTER_NAME_TOKEN = "{{characterName}}"

    /**
     * Parses seeded story entries from JSON and applies text token replacement.
     */
    fun load(context: Context, characterName: String): List<StorySeedEntry> {
        val json = context.assets.open(STORY_SEED_ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }

        val array = JSONArray(json)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    StorySeedEntry(
                        distanceMarker = item.getDouble("distanceMarker"),
                        isPersistent = item.optBoolean("isPersistent", true),
                        isMajor = item.optBoolean("isMajor", false),
                        areaName = item.optString("areaName", ""),
                        text = item.getString("text").replace(CHARACTER_NAME_TOKEN, characterName),
                        unlockedDefault = item.optBoolean("unlockedDefault", false),
                        readDefault = item.optBoolean("readDefault", false)
                    )
                )
            }
        }
    }
}

/**
 * Represents one seeded story entry.
 */
data class StorySeedEntry(

    // Matches milestone distance used for relationship mapping.
    val distanceMarker: Double,

    // Marks milestone row as persistent when true.
    val isPersistent: Boolean,

    // Marks milestone row as major when true.
    val isMajor: Boolean,

    // Stores user-facing area label for the milestone.
    val areaName: String,

    // Stores story body text shown to the user.
    val text: String,

    // Sets initial unlocked state during first-time seeding.
    val unlockedDefault: Boolean,

    // Sets initial read state during first-time seeding.
    val readDefault: Boolean
)