package com.zenox.arrowmaze.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Firestore-facing [com.zenox.arrowmaze.core.domain.model.Profile] DTO.
 *
 * Snake-case [SerialName]s keep the Firestore document schema flat and stable
 * (Firestore field names are immutable once a doc exists). Lists are encoded
 * as JSON arrays natively by kotlinx.serialization — Firestore stores them as
 * `array<string>` fields.
 */
@Serializable
data class ProfileDto(
    @SerialName("uid")                  val uid: String,
    @SerialName("is_guest")             val isGuest: Boolean,
    @SerialName("email")                val email: String? = null,
    @SerialName("display_name")         val displayName: String,
    @SerialName("player_name")          val playerName: String,
    @SerialName("avatar_url")           val avatarUrl: String? = null,
    @SerialName("country")              val country: String,
    @SerialName("join_date_epoch_ms")   val joinDateEpochMs: Long,
    @SerialName("level")                val level: Int,
    @SerialName("xp")                   val xp: Int,
    @SerialName("coins")                val coins: Int,
    @SerialName("hints")                val hints: Int,
    @SerialName("lives")                val lives: Int,
    @SerialName("last_life_regen_epoch_ms") val lastLifeRegenEpochMs: Long,
    @SerialName("games_played")         val gamesPlayed: Int,
    @SerialName("games_won")            val gamesWon: Int,
    @SerialName("best_streak")          val bestStreak: Int,
    @SerialName("current_streak")       val currentStreak: Int,
    @SerialName("average_solve_time_ms") val averageSolveTimeMs: Long,
    @SerialName("highest_level")        val highestLevel: Int,
    @SerialName("current_theme_id")     val currentThemeId: String,
    @SerialName("current_arrow_skin_id") val currentArrowSkinId: String,
    @SerialName("current_trail_fx_id")  val currentTrailFxId: String,
    @SerialName("owned_items")          val ownedItems: List<String> = emptyList(),
    @SerialName("unlocked_achievements") val unlockedAchievements: List<String> = emptyList(),
    @SerialName("is_premium")           val isPremium: Boolean = false,
    @SerialName("is_vip")               val isVip: Boolean = false,
)
