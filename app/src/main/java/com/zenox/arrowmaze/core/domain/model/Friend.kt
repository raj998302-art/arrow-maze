package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lifecycle state of a friend relationship between two players.
 *
 * - [ACCEPTED]         — both sides have agreed; they can see each other's progress.
 * - [PENDING_SENT]     — current user sent a request that hasn't been accepted yet.
 * - [PENDING_RECEIVED] — current user has an incoming request awaiting action.
 * - [BLOCKED]          — current user has blocked the other side; no further interaction.
 */
@Serializable
enum class FriendStatus {
    @SerialName("ACCEPTED")         ACCEPTED,
    @SerialName("PENDING_SENT")     PENDING_SENT,
    @SerialName("PENDING_RECEIVED") PENDING_RECEIVED,
    @SerialName("BLOCKED")          BLOCKED
}

/**
 * A friend / social-graph row. Lives on the friends screen and in the friends leaderboard.
 */
@Serializable
data class Friend(
    @SerialName("uid")               val uid: String,
    @SerialName("playerName")        val playerName: String,
    @SerialName("displayName")       val displayName: String,
    @SerialName("avatarUrl")         val avatarUrl: String?,
    @SerialName("country")           val country: String,
    @SerialName("level")             val level: Int,
    @SerialName("xp")                val xp: Int,
    @SerialName("coins")             val coins: Int,
    @SerialName("isOnline")          val isOnline: Boolean,
    @SerialName("lastSeenEpochMs")   val lastSeenEpochMs: Long,
    @SerialName("status")            val status: FriendStatus
) {
    /** Convenience: can the player challenge / message this friend? */
    val isActionable: Boolean get() = status == FriendStatus.ACCEPTED
}

/**
 * A pending friend request. Stored in Firestore under
 * [com.zenox.arrowmaze.core.common.AppConstants.FS_FRIEND_REQUESTS].
 */
@Serializable
data class FriendRequest(
    @SerialName("id")                val id: String,
    @SerialName("fromUid")           val fromUid: String,
    @SerialName("toUid")             val toUid: String,
    @SerialName("fromName")          val fromName: String,
    @SerialName("timestampEpochMs")  val timestampEpochMs: Long,
    @SerialName("message")           val message: String?
)
