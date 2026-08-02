package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.mapper.FriendMapper.toDomain
import com.zenox.arrowmaze.core.data.mapper.FriendMapper.toEntity
import com.zenox.arrowmaze.core.data.repository.FriendRepository
import com.zenox.arrowmaze.core.database.dao.FriendDao
import com.zenox.arrowmaze.core.database.dao.FriendRequestDao
import com.zenox.arrowmaze.core.database.entity.FriendEntity
import com.zenox.arrowmaze.core.database.entity.FriendRequestEntity
import com.zenox.arrowmaze.core.domain.model.Friend
import com.zenox.arrowmaze.core.domain.model.FriendRequest
import com.zenox.arrowmaze.core.domain.model.FriendStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Offline-first [FriendRepository]. The local cache holds the player's
 * accepted friends + pending requests; real Firestore sync lands in Phase 9/10.
 *
 * All Room rows implicitly belong to the current user — only one user is ever
 * logged in per device, so we don't carry an owner-uid column.
 */
class FriendRepositoryImpl @Inject constructor(
    private val friendDao: FriendDao,
    private val friendRequestDao: FriendRequestDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : FriendRepository {

    override fun observeFriends(): Flow<List<Friend>> =
        friendDao.observeByStatus(FriendStatus.ACCEPTED.name).map { rows -> rows.map { it.toDomain() } }

    override fun observeIncomingRequests(uid: String): Flow<List<FriendRequest>> =
        friendRequestDao.observeIncoming(uid).map { rows -> rows.map { it.toDomain() } }

    override fun observeOutgoingRequests(uid: String): Flow<List<FriendRequest>> =
        friendRequestDao.observeOutgoing(uid).map { rows -> rows.map { it.toDomain() } }

    override suspend fun sendRequest(fromUid: String, toUid: String, message: String?): Result<FriendRequest> =
        withContext(io) {
            resultOf {
                require(fromUid != toUid) { "Cannot send a friend request to yourself" }
                val requestId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val entity = FriendRequestEntity(
                    id = requestId,
                    fromUid = fromUid,
                    toUid = toUid,
                    fromName = fromUid, // Phase 9 will resolve the display name from Firestore.
                    timestampEpochMs = now,
                    message = message,
                )
                friendRequestDao.upsert(entity)
                // Best-effort: also cache the friend row as PENDING_SENT.
                friendDao.upsert(
                    FriendEntity(
                        uid = toUid,
                        playerName = toUid,
                        displayName = toUid,
                        avatarUrl = null,
                        country = "US",
                        level = 1,
                        xp = 0,
                        coins = 0,
                        isOnline = false,
                        lastSeenEpochMs = now,
                        status = FriendStatus.PENDING_SENT.name,
                    )
                )
                Timber.i("Friend request sent: from=%s to=%s id=%s", fromUid, toUid, requestId)
                // Firestore sync: Phase 10
                entity.toDomain()
            }
        }

    override suspend fun acceptRequest(requestId: String, currentUid: String): Result<Unit> =
        withContext(io) {
            resultOf {
                val request = friendRequestDao.get(requestId)
                    ?: throw NoSuchElementException("No friend request with id=$requestId")
                require(request.toUid == currentUid) {
                    "Cannot accept a request that wasn't sent to you"
                }
                // Mark the friend row as ACCEPTED (or insert one if missing).
                val existing = friendDao.get(request.fromUid)
                val now = System.currentTimeMillis()
                val accepted = (existing?.copy(status = FriendStatus.ACCEPTED.name, lastSeenEpochMs = now)
                    ?: FriendEntity(
                        uid = request.fromUid,
                        playerName = request.fromName,
                        displayName = request.fromName,
                        avatarUrl = null,
                        country = "US",
                        level = 1,
                        xp = 0,
                        coins = 0,
                        isOnline = false,
                        lastSeenEpochMs = now,
                        status = FriendStatus.ACCEPTED.name,
                    ))
                friendDao.upsert(accepted)
                friendRequestDao.delete(requestId)
                Timber.i("Friend request accepted: id=%s from=%s", requestId, request.fromUid)
                // Firestore sync: Phase 10
            }
        }

    override suspend fun declineRequest(requestId: String): Result<Unit> = withContext(io) {
        resultOf {
            val request = friendRequestDao.get(requestId)
            if (request != null) {
                friendRequestDao.delete(requestId)
                // Also clean up the cached PENDING_* friend row.
                friendDao.get(request.fromUid)?.let { existing ->
                    if (existing.status == FriendStatus.PENDING_RECEIVED.name ||
                        existing.status == FriendStatus.PENDING_SENT.name
                    ) {
                        friendDao.delete(request.fromUid)
                    }
                }
                Timber.i("Friend request declined: id=%s", requestId)
            }
            // Firestore sync: Phase 10
        }
    }

    override suspend fun removeFriend(uid: String): Result<Unit> = withContext(io) {
        resultOf {
            friendDao.delete(uid)
            Timber.i("Friend removed: uid=%s", uid)
            // Firestore sync: Phase 10
        }
    }

    override suspend fun blockPlayer(uid: String): Result<Unit> = withContext(io) {
        resultOf {
            val existing = friendDao.get(uid)
            val now = System.currentTimeMillis()
            val blocked = existing?.copy(status = FriendStatus.BLOCKED.name, lastSeenEpochMs = now)
                ?: FriendEntity(
                    uid = uid,
                    playerName = uid,
                    displayName = uid,
                    avatarUrl = null,
                    country = "US",
                    level = 1,
                    xp = 0,
                    coins = 0,
                    isOnline = false,
                    lastSeenEpochMs = now,
                    status = FriendStatus.BLOCKED.name,
                )
            friendDao.upsert(blocked)
            Timber.i("Player blocked: uid=%s", uid)
            // Firestore sync: Phase 10
        }
    }

    override suspend fun searchPlayers(query: String): Result<List<Friend>> = withContext(io) {
        resultOf {
            val all = friendDao.getAll().map { it.toDomain() }
            if (query.isBlank()) return@resultOf all
            val q = query.trim().lowercase()
            all.filter {
                it.playerName.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.uid.lowercase().contains(q)
            }
            // Firestore sync: Phase 10 (real prefix query)
        }
    }

    private fun FriendRequestEntity.toDomain(): FriendRequest = FriendRequest(
        id = id,
        fromUid = fromUid,
        toUid = toUid,
        fromName = fromName,
        timestampEpochMs = timestampEpochMs,
        message = message,
    )
}
