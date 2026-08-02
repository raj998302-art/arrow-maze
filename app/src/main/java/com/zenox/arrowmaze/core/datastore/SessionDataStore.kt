package com.zenox.arrowmaze.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Session-scoped state. Lives in DataStore (not memory) so that the app can
 * survive a process death mid-game and restore "you are logged in as X" on
 * the next cold start. Cleared on sign-out.
 */
class SessionDataStore(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val CURRENT_UID = stringPreferencesKey("current_uid")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val HAS_COMPLETED_AUTH = booleanPreferencesKey("has_completed_auth")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val LAST_INTERSTITIAL_EPOCH_MS = longPreferencesKey("last_interstitial_epoch_ms")
    }

    val currentUidFlow: Flow<String?> = dataStore.data.map { it[Keys.CURRENT_UID] }
    val isGuestFlow: Flow<Boolean> = dataStore.data.map { it[Keys.IS_GUEST] ?: false }
    val hasCompletedAuthFlow: Flow<Boolean> = dataStore.data.map { it[Keys.HAS_COMPLETED_AUTH] ?: false }
    val fcmTokenFlow: Flow<String?> = dataStore.data.map { it[Keys.FCM_TOKEN] }
    val lastInterstitialEpochMsFlow: Flow<Long> = dataStore.data.map { it[Keys.LAST_INTERSTITIAL_EPOCH_MS] ?: 0L }

    suspend fun setCurrentUid(uid: String?) {
        dataStore.edit { prefs ->
            if (uid == null) prefs.remove(Keys.CURRENT_UID) else prefs[Keys.CURRENT_UID] = uid
        }
    }

    suspend fun setIsGuest(value: Boolean) {
        dataStore.edit { it[Keys.IS_GUEST] = value }
    }

    suspend fun setHasCompletedAuth(value: Boolean) {
        dataStore.edit { it[Keys.HAS_COMPLETED_AUTH] = value }
    }

    suspend fun setFcmToken(token: String?) {
        dataStore.edit { prefs ->
            if (token == null) prefs.remove(Keys.FCM_TOKEN) else prefs[Keys.FCM_TOKEN] = token
        }
    }

    suspend fun setLastInterstitialEpochMs(epochMs: Long) {
        dataStore.edit { it[Keys.LAST_INTERSTITIAL_EPOCH_MS] = epochMs }
    }

    /** Wipes all session keys. Used on sign-out / account switch. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
