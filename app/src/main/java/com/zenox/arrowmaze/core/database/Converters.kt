package com.zenox.arrowmaze.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room type converters for the JSON-encoded collections used inside entities:
 *
 * - `List<String>` ↔ JSON `String` (used by ProfileEntity for ownedItems /
 *   unlockedAchievements when the mapper hands Room a raw JSON blob; declared
 *   here for symmetry and for any future entity that needs the same shape).
 * - `Map<Int, Long>` ↔ JSON `String` (used by StatsEntity for solveTimesByLevel).
 *
 * The actual on-the-wire encoding is performed by kotlinx.serialization using a
 * lenient, ignore-unknown-keys [Json] instance so that future schema additions
 * don't break existing rows.
 */
class Converters {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val listStringSerializer = ListSerializer(String.serializer())
    private val mapIntLongSerializer = MapSerializer(Int.serializer(), Long.serializer())

    // ----- List<String> -----

    @TypeConverter
    fun listStringToJson(value: List<String>?): String? {
        if (value == null) return null
        return json.encodeToString(listStringSerializer, value)
    }

    @TypeConverter
    fun jsonToListString(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString(listStringSerializer, value) }
            .getOrElse { emptyList() }
    }

    // ----- Map<Int, Long> -----

    @TypeConverter
    fun mapIntLongToJson(value: Map<Int, Long>?): String? {
        if (value == null) return null
        return json.encodeToString(mapIntLongSerializer, value)
    }

    @TypeConverter
    fun jsonToMapIntLong(value: String?): Map<Int, Long>? {
        if (value.isNullOrEmpty()) return emptyMap()
        return runCatching { json.decodeFromString(mapIntLongSerializer, value) }
            .getOrElse { emptyMap() }
    }
}
