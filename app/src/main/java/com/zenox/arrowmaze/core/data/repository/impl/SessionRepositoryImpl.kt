package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.datastore.ProgressDataStore
import com.zenox.arrowmaze.core.datastore.SessionDataStore
import com.zenox.arrowmaze.core.domain.model.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete [SessionRepository]. Pure wrapper over [SessionDataStore] +
 * [ProgressDataStore]; no Room involvement. Writes are dispatched onto IO.
 */
class SessionRepositoryImpl @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val progressDataStore: ProgressDataStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SessionRepository {

    override val currentUidFlow: Flow<String?> = sessionDataStore.currentUidFlow
    override val isGuestFlow: Flow<Boolean> = sessionDataStore.isGuestFlow
    override val hasCompletedAuthFlow: Flow<Boolean> = sessionDataStore.hasCompletedAuthFlow
    override val fcmTokenFlow: Flow<String?> = sessionDataStore.fcmTokenFlow
    override val lastInterstitialEpochMsFlow: Flow<Long> = sessionDataStore.lastInterstitialEpochMsFlow

    override val currentLevelFlow: Flow<Int> = progressDataStore.currentLevelFlow
    override val guestUidFlow: Flow<String?> = progressDataStore.guestUidFlow
    override val guestProfileFlow: Flow<Profile?> =
        progressDataStore.guestProfileJsonFlow.map { progressDataStore.parseGuestProfile(it) }

    override suspend fun setCurrentUid(uid: String?): Result<Unit> = withContext(io) {
        resultOf { sessionDataStore.setCurrentUid(uid) }
    }

    override suspend fun setIsGuest(value: Boolean): Result<Unit> = withContext(io) {
        resultOf { sessionDataStore.setIsGuest(value) }
    }

    override suspend fun setHasCompletedAuth(value: Boolean): Result<Unit> = withContext(io) {
        resultOf { sessionDataStore.setHasCompletedAuth(value) }
    }

    override suspend fun setFcmToken(token: String?): Result<Unit> = withContext(io) {
        resultOf { sessionDataStore.setFcmToken(token) }
    }

    override suspend fun setLastInterstitialEpochMs(epochMs: Long): Result<Unit> = withContext(io) {
        resultOf { sessionDataStore.setLastInterstitialEpochMs(epochMs) }
    }

    override suspend fun setCurrentLevel(level: Int): Result<Unit> = withContext(io) {
        resultOf { progressDataStore.setCurrentLevel(level) }
    }

    override suspend fun setGuestUid(uid: String): Result<Unit> = withContext(io) {
        resultOf { progressDataStore.setGuestUid(uid) }
    }

    override suspend fun setGuestProfile(profile: Profile): Result<Unit> = withContext(io) {
        resultOf {
            val json = progressDataStore.encodeGuestProfile(profile)
            progressDataStore.setGuestProfileJson(json)
        }
    }

    override suspend fun clearGuest(): Result<Unit> = withContext(io) {
        resultOf { progressDataStore.clearGuest() }
    }

    override suspend fun clear(): Result<Unit> = withContext(io) {
        resultOf {
            sessionDataStore.clear()
            progressDataStore.clearGuest()
        }
    }
}
