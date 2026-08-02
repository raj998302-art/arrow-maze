package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import kotlinx.coroutines.flow.Flow

/**
 * Daily-challenge repository. Daily challenges are deterministic per (date, tier):
 * the seed is derived from the ISO date so every player on the same day gets
 * the same board, and the same challenge can be re-generated offline.
 */
interface DailyChallengeRepository {

    /** Reactive stream of every persisted daily challenge (most-recent first). */
    fun observeAll(): Flow<List<DailyChallenge>>

    /**
     * Returns today's challenge, generating it deterministically if it doesn't
     * exist in Room yet. The seed is `today.toEpochDays()`; the tier rotates
     * weekly based on the day-of-year.
     */
    suspend fun getToday(): Result<DailyChallenge>

    /** Returns the most recent persisted challenge (or null if none). */
    suspend fun getLatest(): DailyChallenge?

    /**
     * Marks the challenge for [dateIso] as completed with [solvedInSeconds]
     * and the resulting [streakAfter] count. Idempotent.
     */
    suspend fun markCompleted(dateIso: String, solvedInSeconds: Int, streakAfter: Int): Result<Unit>
}
