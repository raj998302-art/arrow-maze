package com.zenox.arrowmaze.features.authentication

/**
 * UI state for the Auth screen. Forward-only lifecycle:
 *
 *  - [Idle]         — initial state; user has not attempted auth yet.
 *  - [Loading]      — auth call in flight; buttons disabled + spinner shown.
 *  - [Error]        — last auth attempt failed; surfaced as a snackbar.
 *  - [Authenticated]— auth succeeded; the screen navigates to Home.
 */
sealed interface AuthUiState {

    data object Idle : AuthUiState

    data class Loading(val message: String? = null) : AuthUiState

    data class Error(val message: String) : AuthUiState

    data class Authenticated(val uid: String) : AuthUiState
}

/**
 * One-shot navigation / UI events emitted by [AuthViewModel] via a
 * `SharedFlow`. Collected with `LaunchedEffect` on the Auth screen.
 */
sealed interface AuthNavEvent {

    /** The user is fully authenticated — navigate to Home. */
    data object NavigateHome : AuthNavEvent

    /** Show a transient toast (e.g. "Password reset email sent"). */
    data class ShowToast(val message: String) : AuthNavEvent

    /** Open the forgot-password flow (kept for future dialog rerouting). */
    data object NavigateForgotPassword : AuthNavEvent
}
