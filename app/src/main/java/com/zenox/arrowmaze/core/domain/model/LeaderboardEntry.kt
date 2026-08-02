package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Time / scope filter for the leaderboards screen. The backend indexes each scope
 * independently so switching tabs is O(1) on the client.
 */
@Serializable
enum class LeaderboardScope {
    @SerialName("GLOBAL")   GLOBAL,
    @SerialName("FRIENDS")  FRIENDS,
    @SerialName("WEEKLY")   WEEKLY,
    @SerialName("MONTHLY")  MONTHLY,
    @SerialName("ALL_TIME") ALL_TIME
}

/**
 * One row in a leaderboard. [rank] is 1-based and computed server-side; [isCurrentUser]
 * is set by the client after merging so the UI can highlight the player's own row.
 */
@Serializable
data class LeaderboardEntry(
    @SerialName("rank")           val rank: Int,
    @SerialName("uid")            val uid: String,
    @SerialName("playerName")     val playerName: String,
    @SerialName("displayName")    val displayName: String,
    @SerialName("avatarUrl")      val avatarUrl: String?,
    @SerialName("country")        val country: String,
    @SerialName("level")          val level: Int,
    @SerialName("xp")             val xp: Int,
    @SerialName("coins")          val coins: Int,
    @SerialName("highestLevel")   val highestLevel: Int,
    @SerialName("isCurrentUser")  val isCurrentUser: Boolean
) {
    /** Trophy emoji helper for the top three spots; `""` for everyone else. */
    val medal: String
        get() = when (rank) {
            1    -> "🥇"
            2    -> "🥈"
            3    -> "🥉"
            else -> ""
        }
}
