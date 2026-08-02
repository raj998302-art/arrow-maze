package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * High-level bucket an [Achievement] belongs to. Drives the achievements screen tab layout.
 */
@Serializable
enum class AchievementCategory {
    @SerialName("GAMEPLAY")    GAMEPLAY,
    @SerialName("PROGRESSION") PROGRESSION,
    @SerialName("COLLECTION")  COLLECTION,
    @SerialName("SOCIAL")      SOCIAL,
    @SerialName("SPECIAL")     SPECIAL
}

/**
 * Polymorphic unlock condition for an [Achievement]. Each variant is evaluated by the
 * achievements engine against the live [Profile] / [GameStats].
 *
 * The sealed hierarchy keeps the requirement schema explicit so the backend and the client
 * never disagree about what an achievement asks for.
 */
@Serializable
sealed interface AchievementRequirement {

    @Serializable
    @SerialName("solveLevels")
    data class SolveLevels(@SerialName("count") val count: Int) : AchievementRequirement

    @Serializable
    @SerialName("reachLevel")
    data class ReachLevel(@SerialName("level") val level: Int) : AchievementRequirement

    @Serializable
    @SerialName("winWithoutHints")
    data object WinWithoutHints : AchievementRequirement

    @Serializable
    @SerialName("solveUnderTime")
    data class SolveUnderTime(@SerialName("seconds") val seconds: Int) : AchievementRequirement

    @Serializable
    @SerialName("dailyStreak")
    data class DailyStreak(@SerialName("days") val days: Int) : AchievementRequirement

    @Serializable
    @SerialName("useCoins")
    data class UseCoins(@SerialName("amount") val amount: Int) : AchievementRequirement

    @Serializable
    @SerialName("ownItems")
    data class OwnItems(
        @SerialName("category") val category: ShopCategory,
        @SerialName("count")    val count: Int
    ) : AchievementRequirement

    @Serializable
    @SerialName("playGames")
    data class PlayGames(@SerialName("count") val count: Int) : AchievementRequirement

    @Serializable
    @SerialName("winStreak")
    data class WinStreak(@SerialName("count") val count: Int) : AchievementRequirement

    /**
     * Custom requirement whose evaluation is delegated to the achievements engine via
     * [predicateId] (e.g. `"win_legend_with_under_5_moves"`). The engine looks up the
     * matching predicate in a registry.
     */
    @Serializable
    @SerialName("custom")
    data class Custom(@SerialName("predicateId") val predicateId: String) : AchievementRequirement
}

/**
 * Definition of a single achievement. The achievements engine holds the canonical list;
 * a player's unlocked achievement ids live on [Profile.unlockedAchievements].
 *
 * @property id           Stable unique id (e.g. `"first_win"`).
 * @property title        Player-facing title.
 * @property description  Player-facing description, e.g. "Win 10 levels in a row."
 * @property iconAsset    Asset path / Compose vector key for the badge icon.
 * @property category     Tab grouping on the achievements screen.
 * @property requirement  Unlock condition (see [AchievementRequirement]).
 * @property xpReward     XP granted on first unlock.
 * @property coinReward   Coins granted on first unlock.
 * @property isHidden     Hidden until unlocked (spoiler-free achievements).
 */
@Serializable
data class Achievement(
    @SerialName("id")           val id: String,
    @SerialName("title")        val title: String,
    @SerialName("description")  val description: String,
    @SerialName("iconAsset")    val iconAsset: String,
    @SerialName("category")     val category: AchievementCategory,
    @SerialName("requirement")  val requirement: AchievementRequirement,
    @SerialName("xpReward")     val xpReward: Int,
    @SerialName("coinReward")   val coinReward: Int,
    @SerialName("isHidden")     val isHidden: Boolean
)
