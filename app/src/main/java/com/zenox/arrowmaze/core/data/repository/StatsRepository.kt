package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.GameStats
import kotlinx.coroutines.flow.Flow

/**
 * Reads / writes lifetime [GameStats] and folds a finished game's outcome
 * into the aggregate via [recordGamePlayed].
 */
interface StatsRepository {

    /** One-shot read; returns `GameStats.EMPTY` if no row exists yet. */
    suspend fun getStats(uid: String): Result<GameStats>

    /** Reactive observation; emits `GameStats.EMPTY` if no row exists. */
    fun observeStats(uid: String): Flow<GameStats>

    /** Full-replace write. */
    suspend fun saveStats(uid: String, stats: GameStats): Result<Unit>

    /**
     * Updates the aggregate by one game's outcome. Computes the new win-rate,
     * fastest-solve, and streak inside the repository so callers don't need
     * to re-read the row first.
     */
    suspend fun recordGamePlayed(
        uid: String,
        won: Boolean,
        timeMs: Long,
        moves: Int,
        hintsUsed: Int,
        level: Int,
    ): Result<GameStats>
}
