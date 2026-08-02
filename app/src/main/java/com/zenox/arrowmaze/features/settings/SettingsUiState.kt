package com.zenox.arrowmaze.features.settings

import com.zenox.arrowmaze.core.data.repository.UserSettings
import com.zenox.arrowmaze.core.firebase.auth.AuthUser

/**
 * UI state for the Settings screen.
 *
 * - [Loading] — initial settings flow has not emitted yet.
 * - [Success] — settings + (optional) current auth user are available.
 * - [Error]  — the settings flow threw.
 */
sealed interface SettingsUiState {

    data object Loading : SettingsUiState

    data class Success(
        val settings: UserSettings,
        val authUser: AuthUser?,
    ) : SettingsUiState

    data class Error(val message: String) : SettingsUiState
}

/** One-shot UI events emitted by [SettingsViewModel]. */
sealed interface SettingsUiEvent {
    data object SignedOut : SettingsUiEvent
    data object AccountDeleted : SettingsUiEvent
    data class ShowToast(val message: String) : SettingsUiEvent
}
