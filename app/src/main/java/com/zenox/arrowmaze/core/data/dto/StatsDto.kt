package com.zenox.arrowmaze.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Firestore-facing [com.zenox.arrowmaze.core.domain.model.GameStats] DTO.
 * Stored as a sub-collection under `users/{uid}/stats`.
 */
@Serializable
data class StatsDto(
    @SerialName("total_games")          val totalGames: Int,
    @SerialName("total_wins")           val totalWins: Int,
    @SerialName("total_losses")         val totalLosses: Int,
    @SerialName("total_time_ms")        val totalTimeMs: Long,
    @SerialName("total_moves")          val totalMoves: Int,
    @SerialName("total_hints_used")     val totalHintsUsed: Int,
    @SerialName("fastest_solve_ms")     val fastestSolveMs: Long,
    @SerialName("best_streak")          val bestStreak: Int,
    @SerialName("current_streak")       val currentStreak: Int,
    @SerialName("average_solve_time_ms") val averageSolveTimeMs: Long,
    @SerialName("win_rate")             val winRate: Float,
    @SerialName("solve_times_by_level") val solveTimesByLevel: Map<Int, Long> = emptyMap(),
)
