package com.zenox.arrowmaze.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Firestore-facing [com.zenox.arrowmaze.core.domain.model.LeaderboardEntry] DTO.
 *
 * `rank` and `isCurrentUser` are *not* persisted: rank is computed by the
 * leaderboard query's `orderBy(xp).limit(N)` cursor, and `isCurrentUser` is
 * stamped in by the client after merging. The DTO therefore carries only the
 * player-visible fields that actually live in the leaderboard document.
 */
@Serializable
data class LeaderboardEntryDto(
    @SerialName("uid")            val uid: String,
    @SerialName("player_name")    val playerName: String,
    @SerialName("display_name")   val displayName: String,
    @SerialName("avatar_url")     val avatarUrl: String? = null,
    @SerialName("country")        val country: String,
    @SerialName("level")          val level: Int,
    @SerialName("xp")             val xp: Int,
    @SerialName("coins")          val coins: Int,
    @SerialName("highest_level")  val highestLevel: Int,
)
