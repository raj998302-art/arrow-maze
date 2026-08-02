package com.zenox.arrowmaze.features.friends

import com.zenox.arrowmaze.core.domain.model.Friend
import com.zenox.arrowmaze.core.domain.model.FriendRequest

/**
 * UI state for the Friends screen.
 *
 * - [Loading] — initial combine of the friends + requests flows.
 * - [Success] — all three streams (friends, requests, search) have resolved
 *   at least once and are now hot.
 * - [Error]  — one of the streams threw.
 */
sealed interface FriendsUiState {

    data object Loading : FriendsUiState

    data class Success(
        val friends: List<Friend> = emptyList(),
        val incomingRequests: List<FriendRequest> = emptyList(),
        val outgoingRequests: List<FriendRequest> = emptyList(),
        val searchResults: List<Friend> = emptyList(),
        val searchQuery: String = "",
        val isSearching: Boolean = false,
        val isSyncing: Boolean = false,
    ) : FriendsUiState

    data class Error(val message: String) : FriendsUiState
}

/** One-shot UI events emitted from the [FriendsViewModel]. */
sealed interface FriendsUiEvent {
    data class ShowToast(val message: String) : FriendsUiEvent
    data object FriendRemoved : FriendsUiEvent
    data object PlayerBlocked : FriendsUiEvent
    data object RequestSent : FriendsUiEvent
    data object RequestAccepted : FriendsUiEvent
    data object RequestDeclined : FriendsUiEvent
}
