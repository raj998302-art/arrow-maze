package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.repository.LeaderboardRepository
import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry
import com.zenox.arrowmaze.core.domain.model.LeaderboardScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * Phase 5 stub [LeaderboardRepository]. Returns deterministic dummy data so
 * the leaderboard UI can be built and tested before the real Firestore query
 * lands in Phase 9/10.
 *
 * The dummy data is seeded by [LeaderboardScope] + the current user's uid so
 * the same user always sees the same leaderboard (useful for screenshot tests).
 */
class LeaderboardRepositoryImpl @Inject constructor(
    @IoDispatcher private val io: CoroutineDispatcher,
) : LeaderboardRepository {

    override suspend fun getLeaderboard(scope: LeaderboardScope, uid: String): Result<List<LeaderboardEntry>> =
        withContext(io) {
            resultOf {
                Timber.d("Generating dummy leaderboard: scope=%s uid=%s", scope, uid)
                generateDummy(scope, uid)
                // Real Firestore query: Phase 9/10
            }
        }

    override suspend fun refresh(scope: LeaderboardScope, uid: String): Result<List<LeaderboardEntry>> =
        withContext(io) {
            resultOf { generateDummy(scope, uid) }
        }

    /**
     * Generates a deterministic 50-row leaderboard for [scope] and stamps the
     * row whose uid matches [uid] with `isCurrentUser = true`. The current
     * user is always placed in the top 30 so they can see themselves on the
     * first page without scrolling.
     */
    private fun generateDummy(scope: LeaderboardScope, uid: String): List<LeaderboardEntry> {
        val rng = Random(scope.hashCode() xor uid.hashCode())
        val currentUserRank = 5 + rng.nextInt(25)
        val rows = mutableListOf<LeaderboardEntry>()
        val namePrefixes = listOf("Arrow", "Maze", "Puzzle", "Path", "Trail", "Spin", "Solve", "Genius", "Swift", "Bright")
        val nameSuffixes = listOf("King", "Queen", "Master", "Ace", "Pro", "Star", "Hero", "Legend", "Ninja", "Wizard")
        val countries = listOf("US", "GB", "CA", "AU", "DE", "FR", "JP", "BR", "IN", "KR", "ES", "IT", "MX", "RU", "CN")

        for (rank in 1..50) {
            val entryUid = if (rank == currentUserRank) uid else "dummy_uid_$rank"
            val playerName = "${namePrefixes.random(rng)}${nameSuffixes.random(rng)}$rank"
            val xp = (50_000 - rank * 800).coerceAtLeast(100)
            val level = (xp / 1000) + 1
            val coins = rng.nextInt(100, 10000)
            val highestLevel = (rank * 5 + rng.nextInt(0, 20)).coerceAtLeast(1)
            rows += LeaderboardEntry(
                rank = rank,
                uid = entryUid,
                playerName = playerName,
                displayName = playerName,
                avatarUrl = null,
                country = countries.random(rng),
                level = level,
                xp = xp,
                coins = coins,
                highestLevel = highestLevel,
                isCurrentUser = rank == currentUserRank,
            )
        }
        return rows
    }
}
