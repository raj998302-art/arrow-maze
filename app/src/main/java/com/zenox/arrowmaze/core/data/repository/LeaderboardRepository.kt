package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry
import com.zenox.arrowmaze.core.domain.model.LeaderboardScope

/**
 * Leaderboard repository. Phase 5 returns either a local cache or generated
 * dummy data; the real Firestore query is wired in Phase 9/10.
 *
 * Each scope maps to a different Firestore collection / query; the repository
 * hides that detail from callers.
 */
interface LeaderboardRepository {

    /**
     * Returns the leaderboard for [scope]. [uid] is the current user's uid —
     * used to mark the [LeaderboardEntry.isCurrentUser] flag on the matching
     * row.
     */
    suspend fun getLeaderboard(scope: LeaderboardScope, uid: String): Result<List<LeaderboardEntry>>

    /**
     * Force-refreshes the cache for [scope]. The Phase 5 stub returns the
     * cached/generated data unchanged.
     */
    suspend fun refresh(scope: LeaderboardScope, uid: String): Result<List<LeaderboardEntry>>
}
