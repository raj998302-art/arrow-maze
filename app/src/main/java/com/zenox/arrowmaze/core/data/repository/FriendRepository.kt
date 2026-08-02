package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.Friend
import com.zenox.arrowmaze.core.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow

/**
 * Social-graph repository. Local Room cache + best-effort Firestore sync.
 *
 * All [Friend] rows in Room belong to the current user — the entity does not
 * carry an owner uid because only one user is ever logged in at a time on a
 * given device. The repository is intentionally current-user-scoped.
 */
interface FriendRepository {

    /** Reactive stream of accepted friends. */
    fun observeFriends(): Flow<List<Friend>>

    /** Reactive stream of incoming friend requests for [uid]. */
    fun observeIncomingRequests(uid: String): Flow<List<FriendRequest>>

    /** Reactive stream of outgoing friend requests from [uid]. */
    fun observeOutgoingRequests(uid: String): Flow<List<FriendRequest>>

    /** Sends a friend request from [fromUid] to [toUid]. */
    suspend fun sendRequest(fromUid: String, toUid: String, message: String?): Result<FriendRequest>

    /** Accepts an incoming friend request and creates the friend row. */
    suspend fun acceptRequest(requestId: String, currentUid: String): Result<Unit>

    /** Declines / revokes a friend request (works for incoming or outgoing). */
    suspend fun declineRequest(requestId: String): Result<Unit>

    /** Removes a friend (un-friending). Idempotent. */
    suspend fun removeFriend(uid: String): Result<Unit>

    /** Blocks a player (sets status to BLOCKED). */
    suspend fun blockPlayer(uid: String): Result<Unit>

    /**
     * Searches for players by display-name or player-name prefix. Returns a
     * best-effort list; the real Firestore query lands in Phase 9/10. For
     * now the implementation queries the local cache.
     */
    suspend fun searchPlayers(query: String): Result<List<Friend>>
}
