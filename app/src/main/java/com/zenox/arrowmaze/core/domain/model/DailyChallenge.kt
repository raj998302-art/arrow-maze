package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One day's daily-challenge record. Daily challenges are deterministic per (date, tier):
 * the [seed] is derived from the ISO date so every player on the same day gets the same board,
 * and the same challenge can be re-generated offline.
 *
 * @property dateIso             ISO-8601 calendar date (`"2024-10-05"`).
 * @property seed                Deterministic seed fed to [com.zenox.arrowmaze.core.domain.engine.PuzzleGenerator].
 * @property tier                Difficulty tier for the day (rotates weekly).
 * @property boardSize           Edge length of the daily board.
 * @property completed           Whether the player has already solved today's challenge.
 * @property rewardCoins         Coin payout for solving.
 * @property rewardXp            XP payout for solving.
 * @property solvedInSeconds     Solver's time if completed; `null` otherwise.
 * @property streakAfter         Daily-streak count after this challenge (0 if not completed / failed).
 */
@Serializable
data class DailyChallenge(
    @SerialName("dateIso")           val dateIso: String,
    @SerialName("seed")              val seed: Long,
    @SerialName("tier")              val tier: DifficultyTier,
    @SerialName("boardSize")         val boardSize: Int,
    @SerialName("completed")         val completed: Boolean,
    @SerialName("rewardCoins")       val rewardCoins: Int,
    @SerialName("rewardXp")          val rewardXp: Int,
    @SerialName("solvedInSeconds")   val solvedInSeconds: Int?,
    @SerialName("streakAfter")       val streakAfter: Int
) {
    /** True if the player finished today's challenge. */
    val isSolved: Boolean get() = completed && solvedInSeconds != null

    companion object {
        /** Default reward schedule; can be overridden by Remote Config. */
        const val DEFAULT_REWARD_COINS = 50
        const val DEFAULT_REWARD_XP = 100
    }
}
