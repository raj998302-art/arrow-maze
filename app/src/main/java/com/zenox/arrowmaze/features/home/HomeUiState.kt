package com.zenox.arrowmaze.features.home

import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import com.zenox.arrowmaze.core.domain.model.DifficultyTier
import com.zenox.arrowmaze.core.domain.model.Profile

/**
 * UI state for the Home screen.
 *
 * The Home screen is the main hub — it surfaces the player's current level,
 * the daily-challenge availability, and quick links to every other feature
 * (Play / Daily / Practice / Shop / Achievements / Leaderboard).
 *
 *  - [Loading] — profile / daily-challenge still being read from the local
 *    cache or Firestore.
 *  - [Success] — everything the home surface needs is available.
 *  - [Error]   — the profile read failed (e.g. signed-out mid-render or
 *    Firestore timeout). The user can retry via [HomeViewModel.refresh].
 */
sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(
        val profile: Profile,
        /** Level the player should resume at (= highest completed + 1, or 1 for new players). */
        val currentLevel: Int,
        /** Tier that [currentLevel] belongs to (resolved via [com.zenox.arrowmaze.core.domain.model.LevelProgression]). */
        val currentTier: DifficultyTier,
        /** Today's daily challenge (null if it hasn't been generated yet). */
        val dailyChallenge: DailyChallenge?,
        /** True when today's daily challenge hasn't been solved yet. */
        val canPlayDaily: Boolean,
        /** Current daily-streak count (0 if none). */
        val dailyStreak: Int,
        /** Lives left (capped at [com.zenox.arrowmaze.core.common.AppConstants.MAX_LIVES]). */
        val lives: Int,
        /** Number of achievements unlocked (for the Achievements mode-card badge). */
        val unlockedAchievements: Int,
        /** Total number of achievements in the catalogue. */
        val totalAchievements: Int,
        /** ISO-8601 date string of today's challenge (`"2024-10-05"`); used for the calendar strip header. */
        val todayIso: String,
        /** Milliseconds until the next life regenerates (0 when at MAX_LIVES or after regen applied). */
        val nextLifeRegenMs: Long,
        /** True when [HomeViewModel.refresh] is in flight. */
        val isRefreshing: Boolean,
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
