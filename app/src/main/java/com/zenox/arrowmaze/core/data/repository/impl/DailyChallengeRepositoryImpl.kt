package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.mapper.DailyChallengeMapper.toDomain
import com.zenox.arrowmaze.core.data.mapper.DailyChallengeMapper.toEntity
import com.zenox.arrowmaze.core.data.repository.DailyChallengeRepository
import com.zenox.arrowmaze.core.database.dao.DailyChallengeDao
import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import com.zenox.arrowmaze.core.domain.model.DifficultyTier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Concrete [DailyChallengeRepository]. Daily challenges are deterministic per
 * (date, tier); the seed is `today.toEpochDays()`. The tier rotates weekly:
 * `(dayOfYear / 7) % DifficultyTier.entries.size`.
 *
 * Firestore sync: Phase 10
 */
class DailyChallengeRepositoryImpl @Inject constructor(
    private val dailyChallengeDao: DailyChallengeDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DailyChallengeRepository {

    override fun observeAll(): Flow<List<DailyChallenge>> =
        dailyChallengeDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getToday(): Result<DailyChallenge> = withContext(io) {
        resultOf {
            val today = LocalDate.now(ZoneOffset.UTC)
            val dateIso = today.toString() // ISO-8601 "2024-10-05"
            val seed = today.toEpochDay().toLong()
            val tierIndex = (today.dayOfYear / 7) % DifficultyTier.entries.size
            val tier = DifficultyTier.entries[tierIndex]
            val boardSize = tier.boardSizeRange.first

            val existing = dailyChallengeDao.getByDate(dateIso)
            val challenge = existing?.toDomain() ?: DailyChallenge(
                dateIso = dateIso,
                seed = seed,
                tier = tier,
                boardSize = boardSize,
                completed = false,
                rewardCoins = DailyChallenge.DEFAULT_REWARD_COINS,
                rewardXp = DailyChallenge.DEFAULT_REWARD_XP,
                solvedInSeconds = null,
                streakAfter = 0,
            )
            if (existing == null) {
                dailyChallengeDao.upsert(challenge.toEntity())
                Timber.d("Generated daily challenge for %s (seed=%d, tier=%s)", dateIso, seed, tier)
                // Firestore sync: Phase 10
            }
            challenge
        }
    }

    override suspend fun getLatest(): DailyChallenge? = withContext(io) {
        dailyChallengeDao.getLatest()?.toDomain()
    }

    override suspend fun markCompleted(dateIso: String, solvedInSeconds: Int, streakAfter: Int): Result<Unit> =
        withContext(io) {
            resultOf {
                val existing = dailyChallengeDao.getByDate(dateIso)
                if (existing == null) {
                    throw NoSuchElementException("No daily challenge for date $dateIso")
                }
                if (existing.completed) {
                    Timber.d("Daily challenge already completed: %s", dateIso)
                    return@resultOf
                }
                dailyChallengeDao.markCompleted(dateIso, solvedInSeconds, streakAfter)
                Timber.i("Daily challenge completed: %s in %ds (streak=%d)", dateIso, solvedInSeconds, streakAfter)
                // Firestore sync: Phase 10
            }
        }
}
