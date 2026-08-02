package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Aggregate lifetime statistics. Computed by the stats engine from a stream of
 * finished [GameSession]s and persisted alongside the [Profile].
 *
 * Every field is an aggregate value (no per-game arrays) so the object stays small and
 * easy to merge across devices. Per-level solve times live in [solveTimesByLevel] for
 * the chart on the Statistics screen.
 */
@Serializable
data class GameStats(
    @SerialName("totalGames")           val totalGames: Int,
    @SerialName("totalWins")            val totalWins: Int,
    @SerialName("totalLosses")          val totalLosses: Int,
    @SerialName("totalTimeMs")          val totalTimeMs: Long,
    @SerialName("totalMoves")           val totalMoves: Int,
    @SerialName("totalHintsUsed")       val totalHintsUsed: Int,
    @SerialName("fastestSolveMs")       val fastestSolveMs: Long,
    @SerialName("bestStreak")           val bestStreak: Int,
    @SerialName("currentStreak")        val currentStreak: Int,
    @SerialName("averageSolveTimeMs")   val averageSolveTimeMs: Long,
    @SerialName("winRate")              val winRate: Float,
    @SerialName("solveTimesByLevel")    val solveTimesByLevel: Map<Int, Long>
) {
    /** Loss count derived when the source didn't track it explicitly. */
    val computedLosses: Int get() = totalGames - totalWins

    /** True if no games have been recorded yet. */
    val isEmpty: Boolean get() = totalGames == 0

    companion object {
        val EMPTY = GameStats(
            totalGames = 0,
            totalWins = 0,
            totalLosses = 0,
            totalTimeMs = 0L,
            totalMoves = 0,
            totalHintsUsed = 0,
            fastestSolveMs = 0L,
            bestStreak = 0,
            currentStreak = 0,
            averageSolveTimeMs = 0L,
            winRate = 0f,
            solveTimesByLevel = emptyMap()
        )
    }
}
