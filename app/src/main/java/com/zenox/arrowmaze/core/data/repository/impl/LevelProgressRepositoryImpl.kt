package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.repository.LevelProgressRepository
import com.zenox.arrowmaze.core.database.dao.LevelProgressDao
import com.zenox.arrowmaze.core.database.entity.LevelProgressEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first [LevelProgressRepository]. [recordCompletion] does the
 * "keep the best fields" merge so callers don't need to read-then-write.
 *
 * Firestore sync: Phase 10
 */
class LevelProgressRepositoryImpl @Inject constructor(
    private val levelProgressDao: LevelProgressDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LevelProgressRepository {

    override fun observeAll(): Flow<List<LevelProgressRepository.LevelProgress>> =
        levelProgressDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByLevel(level: Int): Flow<LevelProgressRepository.LevelProgress?> =
        levelProgressDao.observeByLevel(level).map { it?.toDomain() }

    override suspend fun getByLevel(level: Int): LevelProgressRepository.LevelProgress? =
        withContext(io) {
            levelProgressDao.getByLevel(level)?.toDomain()
        }

    override suspend fun highestCompletedLevel(): Int = withContext(io) {
        levelProgressDao.highestCompletedLevel() ?: 0
    }

    override suspend fun completedCount(): Int = withContext(io) {
        levelProgressDao.completedCount() ?: 0
    }

    override suspend fun recordCompletion(level: Int, moves: Int, timeMs: Long, stars: Int): Result<Unit> =
        withContext(io) {
            resultOf {
                val existing = levelProgressDao.getByLevel(level)
                val merged = if (existing == null) {
                    LevelProgressEntity(
                        level = level,
                        completed = true,
                        bestMoves = moves,
                        bestTimeMs = timeMs,
                        stars = stars,
                    )
                } else {
                    val bestMoves = existing.bestMoves?.let { minOf(it, moves) } ?: moves
                    val bestTime = existing.bestTimeMs?.let { minOf(it, timeMs) } ?: timeMs
                    val bestStars = maxOf(existing.stars, stars)
                    existing.copy(
                        completed = true,
                        bestMoves = bestMoves,
                        bestTimeMs = bestTime,
                        stars = bestStars,
                    )
                }
                levelProgressDao.upsert(merged)
                Timber.d("Recorded completion: level=%d moves=%d time=%dms stars=%d", level, moves, timeMs, stars)
                // Firestore sync: Phase 10
            }
        }

    private fun LevelProgressEntity.toDomain(): LevelProgressRepository.LevelProgress =
        LevelProgressRepository.LevelProgress(
            level = level,
            completed = completed,
            bestMoves = bestMoves,
            bestTimeMs = bestTimeMs,
            stars = stars,
        )
}
