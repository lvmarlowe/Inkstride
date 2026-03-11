package com.inkstride.app.data.database

import android.content.Context
import org.json.JSONArray

object StorySeedDataSource {
    private const val STORY_SEED_ASSET_PATH = "story_seed_data.json"
    private const val CHARACTER_NAME_TOKEN = "{{characterName}}"

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

data class StorySeedEntry(
    val distanceMarker: Double,
    val isPersistent: Boolean,
    val isMajor: Boolean,
    val areaName: String,
    val text: String,
    val unlockedDefault: Boolean,
    val readDefault: Boolean
)