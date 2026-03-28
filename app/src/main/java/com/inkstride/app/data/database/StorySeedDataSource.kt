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

    // Caches parsed raw seed entries so subsequent loads avoid repeated asset I/O and JSON parsing.
    @Volatile
    private var parsedSeedCache: List<RawStorySeedEntry>? = null

    /**
     * load: Parses seeded story entries from JSON and applies character name token replacement.
     * Returns one StorySeedEntry per JSON object for use during database setup.
     */
    fun load(context: Context, characterName: String): List<StorySeedEntry> {
        return getParsedSeedEntries(context).map { rawEntry ->
            StorySeedEntry(
                distanceMarker = rawEntry.distanceMarker,
                isPersistent = rawEntry.isPersistent,
                isMajor = rawEntry.isMajor,
                areaName = rawEntry.areaName,
                text = rawEntry.textTemplate.replace(CHARACTER_NAME_TOKEN, characterName),
                unlockedDefault = rawEntry.unlockedDefault,
                readDefault = rawEntry.readDefault
            )
        }
    }

    // getParsedSeedEntries: Parses raw seed data once and reuses it for subsequent loads.
    private fun getParsedSeedEntries(context: Context): List<RawStorySeedEntry> {
        parsedSeedCache?.let { cached ->
            return cached
        }

        return synchronized(this) {
            parsedSeedCache?.let { cached ->
                return@synchronized cached
            }

            val parsed = parseRawSeedEntries(context)
            parsedSeedCache = parsed
            parsed
        }
    }

    // parseRawSeedEntries: Reads and parses the seed asset into raw entries without token replacement.
    private fun parseRawSeedEntries(context: Context): List<RawStorySeedEntry> {
        val json = context.assets.open(STORY_SEED_ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }

        val array = JSONArray(json)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RawStorySeedEntry(
                        distanceMarker = item.getDouble("distanceMarker"),
                        isPersistent = item.optBoolean("isPersistent", true),
                        isMajor = item.optBoolean("isMajor", false),
                        areaName = item.optString("areaName", ""),
                        textTemplate = item.getString("text"),
                        unlockedDefault = item.optBoolean("unlockedDefault", false),
                        readDefault = item.optBoolean("readDefault", false)
                    )
                )
            }
        }
    }
}

/**
 * RawStorySeedEntry: Stores parsed JSON values before runtime character token replacement is applied.
 * Separates raw asset data from the final StorySeedEntry so token replacement only runs when needed.
 */
private data class RawStorySeedEntry(
    val distanceMarker: Double,
    val isPersistent: Boolean,
    val isMajor: Boolean,
    val areaName: String,
    val textTemplate: String,
    val unlockedDefault: Boolean,
    val readDefault: Boolean
)

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