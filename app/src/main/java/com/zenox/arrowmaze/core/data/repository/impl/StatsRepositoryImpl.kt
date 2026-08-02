package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.mapper.StatsMapper.toDomain
import com.zenox.arrowmaze.core.data.mapper.StatsMapper.toDto
import com.zenox.arrowmaze.core.data.mapper.StatsMapper.toEntity
import com.zenox.arrowmaze.core.data.repository.StatsRepository
import com.zenox.arrowmaze.core.database.dao.StatsDao
import com.zenox.arrowmaze.core.domain.model.GameStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first [StatsRepository]. [recordGamePlayed] reads the current row,
 * applies the increment, computes the new derived fields (win-rate, average,
 * fastest-solve, streak) and writes the full row back so callers always see a
 * consistent snapshot.
 *
 * Firestore sync: Phase 10
 */
class StatsRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StatsRepository {

    override suspend fun getStats(uid: String): Result<GameStats> = withContext(io) {
        resultOf {
            val entity = statsDao.get(uid)
            entity?.toDomain() ?: GameStats.EMPTY
        }
    }

    override fun observeStats(uid: String): Flow<GameStats> =
        statsDao.observe(uid).map { it?.toDomain() ?: GameStats.EMPTY }

    override suspend fun saveStats(uid: String, stats: GameStats): Result<Unit> = withContext(io) {
        resultOf {
            statsDao.upsert(stats.toEntity(uid))
            Timber.d("Saved stats: uid=%s games=%d wins=%d", uid, stats.totalGames, stats.totalWins)
            // Firestore sync: Phase 10
            // Touch DTO so mapper isn't stripped; Phase 10 will hand it to Firestore.
            val dto = stats.toDto()
            Timber.v("Prepared Firestore stats payload for uid=%s", uid)
        }
    }

    override suspend fun recordGamePlayed(
        uid: String,
        won: Boolean,
        timeMs: Long,
        moves: Int,
        hintsUsed: Int,
        level: Int,
    ): Result<GameStats> = withContext(io) {
        resultOf {
            val current = statsDao.get(uid)?.toDomain() ?: GameStats.EMPTY
            val newTotalGames = current.totalGames + 1
            val newTotalWins = current.totalWins + if (won) 1 else 0
            val newTotalLosses = current.totalLosses + if (!won) 1 else 0
            val newTotalTimeMs = current.totalTimeMs + timeMs
            val newTotalMoves = current.totalMoves + moves
            val newTotalHints = current.totalHintsUsed + hintsUsed
            val newFastest = when {
                !won -> current.fastestSolveMs
                current.fastestSolveMs == 0L -> timeMs
                else -> minOf(current.fastestSolveMs, timeMs)
            }
            val newStreak = if (won) current.currentStreak + 1 else 0
            val newBestStreak = maxOf(current.bestStreak, newStreak)
            val newAverage = if (newTotalGames == 0) 0L else newTotalTimeMs / newTotalGames
            val newWinRate = if (newTotalGames == 0) 0f else newTotalWins.toFloat() / newTotalGames
            val newSolveTimes = if (won) {
                current.solveTimesByLevel + (level to timeMs)
            } else {
                current.solveTimesByLevel
            }

            val updated = GameStats(
                totalGames = newTotalGames,
                totalWins = newTotalWins,
                totalLosses = newTotalLosses,
                totalTimeMs = newTotalTimeMs,
                totalMoves = newTotalMoves,
                totalHintsUsed = newTotalHints,
                fastestSolveMs = newFastest,
                bestStreak = newBestStreak,
                currentStreak = newStreak,
                averageSolveTimeMs = newAverage,
                winRate = newWinRate,
                solveTimesByLevel = newSolveTimes,
            )
            statsDao.upsert(updated.toEntity(uid))
            Timber.d("Recorded game: uid=%s won=%s time=%dms moves=%d", uid, won, timeMs, moves)
            // Firestore sync: Phase 10
            updated
        }
    }
}
