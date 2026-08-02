package com.zenox.arrowmaze.features.leaderboard

import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry
import com.zenox.arrowmaze.core.domain.model.LeaderboardScope

/**
 * UI state for the Leaderboard screen.
 *
 * - [Loading] — initial fetch or scope switch in flight.
 * - [Success] — entries for the active [scope] are available; [myRank] is the
 *   1-based rank of the current user (null if they're outside the returned list).
 * - [Error] — the fetch failed; user can retry.
 */
sealed interface LeaderboardUiState {

    data object Loading : LeaderboardUiState

    data class Success(
        val entries: List<LeaderboardEntry>,
        val scope: LeaderboardScope,
        val myRank: Int?,
        val isRefreshing: Boolean = false,
    ) : LeaderboardUiState

    data class Error(val message: String) : LeaderboardUiState
}
