package com.zenox.arrowmaze.features.dailychallenge

import com.zenox.arrowmaze.core.domain.model.DailyChallenge

/**
 * UI state for the Daily Challenge screen.
 */
sealed interface DailyChallengeUiState {

    data object Loading : DailyChallengeUiState

    data class Success(
        val challenge: DailyChallenge,
        val streak: Int,
        val calendar: List<DayStatus>,
        val history: List<DailyChallenge>,
    ) : DailyChallengeUiState

    data class Error(val message: String) : DailyChallengeUiState
}

/**
 * One row in the last-7-days calendar strip.
 *
 * @property dateIso      ISO date string (e.g. "2024-10-05").
 * @property completed    Whether the challenge for this date was solved.
 * @property isToday      Whether this is the current date.
 * @property rewardClaimed Whether the reward was already credited (== completed).
 */
data class DayStatus(
    val dateIso: String,
    val completed: Boolean,
    val isToday: Boolean,
    val rewardClaimed: Boolean,
)

/** One-shot UI events emitted by [DailyChallengeViewModel]. */
sealed interface DailyChallengeNavEvent {
    /** Navigate to the Game screen for today's daily challenge. */
    data object StartChallenge : DailyChallengeNavEvent
    data class ShowToast(val message: String) : DailyChallengeNavEvent
}
