package com.zenox.arrowmaze.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.FriendRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.domain.model.Friend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Friends screen.
 *
 * Combines three reactive streams — accepted friends, incoming requests,
 * outgoing requests — keyed off the current uid. Search is a separate
 * imperative surface because the [FriendRepository.searchPlayers] query is
 * one-shot.
 *
 * All mutations (send / accept / decline / remove / block) are funneled
 * through the repository, after which the observable streams auto-emit the
 * new state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Friend>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)

    private val _uiEvents = Channel<FriendsUiEvent>(capacity = Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    // Bundles the four search/sync MutableStateFlows so we can combine them
    // in a single `combine` call without exceeding the 5-arg overload.
    private data class SearchState(
        val results: List<Friend>,
        val query: String,
        val isSearching: Boolean,
        val isSyncing: Boolean,
    )

    private val searchStream = combine(
        _searchResults,
        _searchQuery,
        _isSearching,
        _isSyncing,
    ) { results, query, searching, syncing ->
        SearchState(results, query, searching, syncing)
    }

    val uiState: StateFlow<FriendsUiState> = sessionRepository.currentUidFlow
        .flatMapLatest { uid ->
            if (uid == null) {
                kotlinx.coroutines.flow.flowOf(FriendsUiState.Error("Not signed in"))
            } else {
                combine(
                    friendRepository.observeFriends(),
                    friendRepository.observeIncomingRequests(uid),
                    friendRepository.observeOutgoingRequests(uid),
                    searchStream,
                ) { friends, incoming, outgoing, search ->
                    FriendsUiState.Success(
                        friends = friends,
                        incomingRequests = incoming,
                        outgoingRequests = outgoing,
                        searchResults = search.results,
                        searchQuery = search.query,
                        isSearching = search.isSearching,
                        isSyncing = search.isSyncing,
                    )
                }
            }
        }
        .catch { t ->
            Timber.e(t, "Friends stream failed")
            emit(FriendsUiState.Error(t.message ?: "Failed to load friends"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FriendsUiState.Loading,
        )

    /** Sends a friend request to [toUid]. The current uid is resolved from the session. */
    fun sendRequest(toUid: String) {
        if (toUid.isBlank()) return
        viewModelScope.launch {
            val fromUid = sessionRepository.currentUidFlow.first() ?: return@launch
            _isSyncing.value = true
            friendRepository.sendRequest(fromUid, toUid, message = null)
                .onSuccess { _uiEvents.send(FriendsUiEvent.RequestSent) }
                .onFailure { error ->
                    Timber.w(error.asException(), "sendRequest failed")
                    _uiEvents.send(FriendsUiEvent.ShowToast(error.message))
                }
            _isSyncing.value = false
        }
    }

    /** Accepts an incoming friend request. */
    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            val currentUid = sessionRepository.currentUidFlow.first() ?: return@launch
            _isSyncing.value = true
            friendRepository.acceptRequest(requestId, currentUid)
                .onSuccess { _uiEvents.send(FriendsUiEvent.RequestAccepted) }
                .onFailure { error ->
                    Timber.w(error.asException(), "acceptRequest failed")
                    _uiEvents.send(FriendsUiEvent.ShowToast(error.message))
                }
            _isSyncing.value = false
        }
    }

    /** Declines / revokes a friend request (works for incoming or outgoing). */
    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            friendRepository.declineRequest(requestId)
                .onSuccess { _uiEvents.send(FriendsUiEvent.RequestDeclined) }
                .onFailure { error ->
                    Timber.w(error.asException(), "declineRequest failed")
                    _uiEvents.send(FriendsUiEvent.ShowToast(error.message))
                }
            _isSyncing.value = false
        }
    }

    /** Removes a friend. */
    fun removeFriend(uid: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            friendRepository.removeFriend(uid)
                .onSuccess { _uiEvents.send(FriendsUiEvent.FriendRemoved) }
                .onFailure { error ->
                    Timber.w(error.asException(), "removeFriend failed")
                    _uiEvents.send(FriendsUiEvent.ShowToast(error.message))
                }
            _isSyncing.value = false
        }
    }

    /** Blocks a player. */
    fun blockPlayer(uid: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            friendRepository.blockPlayer(uid)
                .onSuccess { _uiEvents.send(FriendsUiEvent.PlayerBlocked) }
                .onFailure { error ->
                    Timber.w(error.asException(), "blockPlayer failed")
                    _uiEvents.send(FriendsUiEvent.ShowToast(error.message))
                }
            _isSyncing.value = false
        }
    }

    /**
     * Runs a player search. An empty / blank query clears the results list
     * (the search tab then shows a hint instead of a stale list).
     */
    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            when (val result = friendRepository.searchPlayers(query)) {
                is Result.Success -> _searchResults.value = result.data
                is Result.Failure -> {
                    Timber.w(result.error.asException(), "searchPlayers failed")
                    _uiEvents.send(FriendsUiEvent.ShowToast(result.error.message))
                }
                Result.Loading -> Unit
            }
            _isSearching.value = false
        }
    }
}
