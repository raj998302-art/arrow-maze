package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Persistent player profile. Stored locally in DataStore and mirrored to Firestore.
 * All fields are value types so the entire record round-trips through JSON without
 * custom serializers.
 *
 * The level curve is linear: level N requires `N * [XP_PER_LEVEL]` total XP.
 * [xpProgress] returns the (into-level, remaining-for-next) tuple the HUD renders.
 */
@Serializable
data class Profile(
    @SerialName("uid")                   val uid: String,
    @SerialName("isGuest")               val isGuest: Boolean,
    @SerialName("email")                 val email: String?,
    @SerialName("displayName")           val displayName: String,
    @SerialName("playerName")            val playerName: String,
    @SerialName("avatarUrl")             val avatarUrl: String?,
    @SerialName("country")               val country: String,
    @SerialName("joinDateEpochMs")       val joinDateEpochMs: Long,
    @SerialName("level")                 val level: Int,
    @SerialName("xp")                    val xp: Int,
    @SerialName("coins")                 val coins: Int,
    @SerialName("hints")                 val hints: Int,
    @SerialName("lives")                 val lives: Int,
    @SerialName("lastLifeRegenEpochMs")  val lastLifeRegenEpochMs: Long,
    @SerialName("gamesPlayed")           val gamesPlayed: Int,
    @SerialName("gamesWon")              val gamesWon: Int,
    @SerialName("bestStreak")            val bestStreak: Int,
    @SerialName("currentStreak")         val currentStreak: Int,
    @SerialName("averageSolveTimeMs")    val averageSolveTimeMs: Long,
    @SerialName("highestLevel")          val highestLevel: Int,
    @SerialName("currentThemeId")        val currentThemeId: String,
    @SerialName("currentArrowSkinId")    val currentArrowSkinId: String,
    @SerialName("currentTrailFxId")      val currentTrailFxId: String,
    @SerialName("ownedItems")            val ownedItems: List<String> = emptyList(),
    @SerialName("unlockedAchievements")  val unlockedAchievements: List<String> = emptyList(),
    @SerialName("isPremium")             val isPremium: Boolean = false,
    @SerialName("isVip")                 val isVip: Boolean = false
) {

    /**
     * Returns (xpIntoCurrentLevel, xpStillNeededForNextLevel) based on a flat 1000-XP-per-level
     * curve (matches [com.zenox.arrowmaze.core.common.AppConstants.XP_PER_LEVEL]).
     */
    fun xpProgress(xpPerLevel: Int = DEFAULT_XP_PER_LEVEL): Pair<Int, Int> {
        val into = xp.coerceAtLeast(0) % xpPerLevel
        val remaining = xpPerLevel - into
        return into to remaining
    }

    /** Total XP required to reach [targetLevel] from zero. */
    fun xpRequiredForLevel(targetLevel: Int, xpPerLevel: Int = DEFAULT_XP_PER_LEVEL): Int =
        targetLevel * xpPerLevel

    /** Win-rate as a 0..1 fraction (0.0 when no games played). */
    val winRate: Float
        get() = if (gamesPlayed == 0) 0f else gamesWon.toFloat() / gamesPlayed

    companion object {
        const val DEFAULT_XP_PER_LEVEL: Int = 1000

        /** Factory for a fresh guest profile with sensible defaults. */
        fun guest(uid: String, playerName: String, nowEpochMs: Long): Profile = Profile(
            uid = uid,
            isGuest = true,
            email = null,
            displayName = playerName,
            playerName = playerName,
            avatarUrl = null,
            country = "US",
            joinDateEpochMs = nowEpochMs,
            level = 1,
            xp = 0,
            coins = 100,
            hints = 3,
            lives = 5,
            lastLifeRegenEpochMs = nowEpochMs,
            gamesPlayed = 0,
            gamesWon = 0,
            bestStreak = 0,
            currentStreak = 0,
            averageSolveTimeMs = 0L,
            highestLevel = 0,
            currentThemeId = "dark",
            currentArrowSkinId = "default",
            currentTrailFxId = "default"
        )
    }
}
