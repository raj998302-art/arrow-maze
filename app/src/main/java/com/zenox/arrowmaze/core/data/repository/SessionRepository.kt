package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.Profile
import kotlinx.coroutines.flow.Flow

/**
 * Session-scoped state. Owns the current-user uid, guest-mode flag, FCM token,
 * last-interstitial timestamp, and the guest profile blob.
 *
 * Wraps both [com.zenox.arrowmaze.core.datastore.SessionDataStore] and
 * [com.zenox.arrowmaze.core.datastore.ProgressDataStore] so that session
 * state lives behind a single facade.
 */
interface SessionRepository {

    val currentUidFlow: Flow<String?>
    val isGuestFlow: Flow<Boolean>
    val hasCompletedAuthFlow: Flow<Boolean>
    val fcmTokenFlow: Flow<String?>
    val lastInterstitialEpochMsFlow: Flow<Long>

    val currentLevelFlow: Flow<Int>
    val guestUidFlow: Flow<String?>
    val guestProfileFlow: Flow<Profile?>

    suspend fun setCurrentUid(uid: String?): Result<Unit>
    suspend fun setIsGuest(value: Boolean): Result<Unit>
    suspend fun setHasCompletedAuth(value: Boolean): Result<Unit>
    suspend fun setFcmToken(token: String?): Result<Unit>
    suspend fun setLastInterstitialEpochMs(epochMs: Long): Result<Unit>

    suspend fun setCurrentLevel(level: Int): Result<Unit>
    suspend fun setGuestUid(uid: String): Result<Unit>
    suspend fun setGuestProfile(profile: Profile): Result<Unit>

    /** Clears all guest-related state. */
    suspend fun clearGuest(): Result<Unit>

    /** Clears the entire session (used on sign-out). */
    suspend fun clear(): Result<Unit>
}
