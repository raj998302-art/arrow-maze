package com.zenox.arrowmaze.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Firestore-facing [com.zenox.arrowmaze.core.domain.model.Friend] DTO.
 * Stored under `users/{uid}/friends/{friendUid}`.
 */
@Serializable
data class FriendDto(
    @SerialName("uid")                val uid: String,
    @SerialName("player_name")        val playerName: String,
    @SerialName("display_name")       val displayName: String,
    @SerialName("avatar_url")         val avatarUrl: String? = null,
    @SerialName("country")            val country: String,
    @SerialName("level")              val level: Int,
    @SerialName("xp")                 val xp: Int,
    @SerialName("coins")              val coins: Int,
    @SerialName("is_online")          val isOnline: Boolean,
    @SerialName("last_seen_epoch_ms") val lastSeenEpochMs: Long,
    @SerialName("status")             val status: String,
)
