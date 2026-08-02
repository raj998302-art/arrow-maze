package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Per-level best-result view of the player's progress. Powers the level-select
 * screen's stars + completion state.
 */
interface LevelProgressRepository {

    data class LevelProgress(
        val level: Int,
        val completed: Boolean,
        val bestMoves: Int?,
        val bestTimeMs: Long?,
        val stars: Int,
    )

    /** Reactive stream of all level-progress rows. */
    fun observeAll(): Flow<List<LevelProgress>>

    /** Reactive stream for a single level. */
    fun observeByLevel(level: Int): Flow<LevelProgress?>

    /** One-shot read for a single level. */
    suspend fun getByLevel(level: Int): LevelProgress?

    /** Highest completed level (0 if none). */
    suspend fun highestCompletedLevel(): Int

    /** Total completed levels. */
    suspend fun completedCount(): Int

    /**
     * Records a completion. If the level was already completed, only the
     * best fields are kept (min moves, min time, max stars); otherwise a new
     * row is inserted with `completed = true`.
     */
    suspend fun recordCompletion(level: Int, moves: Int, timeMs: Long, stars: Int): Result<Unit>
}
