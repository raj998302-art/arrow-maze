package com.zenox.arrowmaze.features.achievements

import com.zenox.arrowmaze.core.domain.model.Achievement

/**
 * UI state for the Achievements screen.
 */
sealed interface AchievementsUiState {

    data object Loading : AchievementsUiState

    data class Success(
        val achievements: List<AchievementDisplay>,
        val unlockedCount: Int,
        val totalCount: Int,
        val xpFromAchievements: Int,
    ) : AchievementsUiState

    data class Error(val message: String) : AchievementsUiState
}

/**
 * Display-ready view of an [Achievement] with unlock state + progress.
 */
data class AchievementDisplay(
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val progress: Int,
    val progressTarget: Int,
)

/**
 * One-shot UI events emitted by [AchievementsViewModel].
 */
sealed interface AchievementsNavEvent {
    data object DismissUnlockPopup : AchievementsNavEvent
    data class ShowToast(val message: String) : AchievementsNavEvent
}
