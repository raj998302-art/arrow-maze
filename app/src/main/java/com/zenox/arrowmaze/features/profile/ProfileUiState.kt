package com.zenox.arrowmaze.features.profile

import com.zenox.arrowmaze.core.domain.model.GameStats
import com.zenox.arrowmaze.core.domain.model.Profile

/**
 * UI state for the Profile screen.
 *
 *  - [Loading] — profile is being read from the local cache / Firestore.
 *  - [Success] — profile + stats are available; render the full surface.
 *  - [Error]   — the profile read failed (e.g. signed-out mid-render).
 */
sealed interface ProfileUiState {

    data object Loading : ProfileUiState

    data class Success(
        val profile: Profile,
        val stats: GameStats,
        val isOwnProfile: Boolean,
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}

/**
 * One-shot UI events emitted by [ProfileViewModel] via a `SharedFlow`.
 */
sealed interface ProfileNavEvent {

    /** Account was signed out — caller navigates to the Auth screen. */
    data object SignedOut : ProfileNavEvent

    /** Account was deleted — caller navigates to the Auth screen. */
    data object AccountDeleted : ProfileNavEvent

    /** Show a transient toast (e.g. "Profile updated"). */
    data class ShowToast(val message: String) : ProfileNavEvent
}
