package com.inkstride.app.data.database

import android.content.Context
import org.json.JSONArray

/**
 * StorySeedDataSource: Loads seeded story rows from bundled JSON.
 * Keeps seed parsing in one place so DatabaseProvider stays focused on setup logic.
 *
 * Expected JSON structure per entry:
 *   distanceMarker   - Double, route distance where the milestone and segment are placed
 *   isPersistent     - Boolean (default true), keeps milestone visible after journey resets
 *   isMajor          - Boolean (default false), flags key story moments in the UI
 *   areaName         - String (default ""), user-facing location label for the milestone
 *   text             - String, story body text, may contain {{characterName}} tokens
 *   unlockedDefault  - Boolean (default false), initial unlocked state during first-time seeding
 *   readDefault      - Boolean (default false), initial read state during first-time seeding
 */
object StorySeedDataSource {

    // Points to the bundled seed data asset file loaded at startup.
    private const val STORY_SEED_ASSET_PATH = "story_seed_data.json"

    // Marks the token replaced with the player-selected character name during load.
    private const val CHARACTER_NAME_TOKEN = "{{characterName}}"

    /**
     * load: Parses seeded story entries from JSON and applies character name token replacement.
     * Returns one StorySeedEntry per JSON object for use during database setup.
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
 * StorySeedEntry: Represents one seeded story entry parsed from the bundled JSON asset.
 * Carries milestone metadata and story content used during database setup.
 */
data class StorySeedEntry(

    // Matches milestone distance used for relationship mapping during seed insert.
    val distanceMarker: Double,

    // Marks milestone row as persistent so it survives journey resets.
    val isPersistent: Boolean,

    // Marks milestone row as major to flag key story moments in the UI.
    val isMajor: Boolean,

    // Stores user-facing area label displayed in story and recap views.
    val areaName: String,

    // Stores story body text shown to the user after character name tokens are replaced.
    val text: String,

    // Sets initial unlocked state applied during first-time seeding.
    val unlockedDefault: Boolean,

    // Sets initial read state applied during first-time seeding.
    val readDefault: Boolean
)