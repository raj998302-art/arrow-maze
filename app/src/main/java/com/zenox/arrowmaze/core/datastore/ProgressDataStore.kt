package com.zenox.arrowmaze.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Light-weight progress state that lives outside of Room (because it must
 * survive a sign-out but is also cheap enough to read on every cold start).
 *
 * - `currentLevel`: which level the player should resume on (last attempted + 1).
 * - `guestUid`:     a stable UUID generated once on first guest session, so a
 *                   guest's progress is consistent across app restarts.
 * - `guestProfileJson`: the full guest [com.zenox.arrowmaze.core.domain.model.Profile]
 *                   serialised to JSON, so the guest profile can be restored
 *                   without hitting Room (e.g. before sign-in completes).
 */
class ProgressDataStore(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val CURRENT_LEVEL = intPreferencesKey("current_level")
        val GUEST_UID = stringPreferencesKey("guest_uid")
        val GUEST_PROFILE_JSON = stringPreferencesKey("guest_profile_json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val currentLevelFlow: Flow<Int> = dataStore.data.map { it[Keys.CURRENT_LEVEL] ?: 1 }
    val guestUidFlow: Flow<String?> = dataStore.data.map { it[Keys.GUEST_UID] }
    val guestProfileJsonFlow: Flow<String?> = dataStore.data.map { it[Keys.GUEST_PROFILE_JSON] }

    suspend fun setCurrentLevel(level: Int) {
        dataStore.edit { it[Keys.CURRENT_LEVEL] = level.coerceAtLeast(1) }
    }

    suspend fun setGuestUid(uid: String) {
        dataStore.edit { it[Keys.GUEST_UID] = uid }
    }

    suspend fun setGuestProfileJson(profileJson: String?) {
        dataStore.edit { prefs ->
            if (profileJson == null) prefs.remove(Keys.GUEST_PROFILE_JSON)
            else prefs[Keys.GUEST_PROFILE_JSON] = profileJson
        }
    }

    /**
     * Serialises the supplied profile JSON via the local [Json] instance for
     * safe storage. Returns silently if the JSON is malformed (we never want
     * to crash the caller because of a bad guest blob).
     */
    suspend fun setGuestProfileJsonFromAny(profileJson: String) {
        // Validate by parsing; ignore the result.
        runCatching { json.parseToJsonElement(profileJson) }
            .onFailure { Timber.w(it, "Rejecting malformed guest profile JSON") }
            .onSuccess { setGuestProfileJson(profileJson) }
    }

    /** Clears only the guest-related keys (keeps currentLevel). */
    suspend fun clearGuest() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.GUEST_UID)
            prefs.remove(Keys.GUEST_PROFILE_JSON)
        }
    }

    /** Resets everything, including the current level (used on a full data wipe). */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    /** Parse a guest profile JSON string back into a domain Profile. */
    fun parseGuestProfile(jsonString: String?): com.zenox.arrowmaze.core.domain.model.Profile? {
        if (jsonString.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString(
                com.zenox.arrowmaze.core.domain.model.Profile.serializer(),
                jsonString,
            )
        }.getOrElse {
            Timber.w(it, "Failed to decode guest profile JSON")
            null
        }
    }

    /** Serialise a domain Profile into the JSON form we persist. */
    fun encodeGuestProfile(profile: com.zenox.arrowmaze.core.domain.model.Profile): String {
        return json.encodeToString(
            com.zenox.arrowmaze.core.domain.model.Profile.serializer(),
            profile,
        )
    }
}
