package com.zenox.arrowmaze.features.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.data.repository.LeaderboardRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry
import com.zenox.arrowmaze.core.domain.model.LeaderboardScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Leaderboard screen.
 *
 * Holds the currently selected [LeaderboardScope] as a [MutableStateFlow]
 * and reloads the entries every time the scope changes. The current user's
 * uid is sourced from [SessionRepository]; if there's no signed-in user
 * (shouldn't happen on this screen, but defensive), we fall back to the
 * empty string so the repository can still return a deterministic dummy
 * list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _selectedScope = MutableStateFlow(LeaderboardScope.GLOBAL)
    val selectedScope: StateFlow<LeaderboardScope> = _selectedScope.asStateFlow()

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        // Reload whenever the selected scope changes.
        viewModelScope.launch {
            _selectedScope.collect { scope -> loadScope(scope, isRefresh = false) }
        }
    }

    /** Switches the active scope and reloads. */
    fun selectScope(scope: LeaderboardScope) {
        if (_selectedScope.value == scope) return
        _selectedScope.value = scope
    }

    /** Pull-to-refresh entry point. */
    fun refresh() {
        viewModelScope.launch {
            loadScope(_selectedScope.value, isRefresh = true)
        }
    }

    /** Fetches the leaderboard for [scope] and pushes the result into [_uiState]. */
    private suspend fun loadScope(scope: LeaderboardScope, isRefresh: Boolean) {
        if (isRefresh) {
            val current = _uiState.value
            if (current is LeaderboardUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            }
        } else {
            _uiState.value = LeaderboardUiState.Loading
        }
        val uid = currentUid()
        when (val result = leaderboardRepository.getLeaderboard(scope, uid)) {
            is Result.Success -> {
                val myRank = result.data.firstOrNull { it.isCurrentUser }?.rank
                _uiState.value = LeaderboardUiState.Success(
                    entries = result.data,
                    scope = scope,
                    myRank = myRank,
                    isRefreshing = false,
                )
            }
            is Result.Failure -> {
                Timber.w(result.error.asException(), "Leaderboard fetch failed: scope=%s", scope)
                // On refresh failure, keep the existing data instead of clobbering it with an error.
                val existing = _uiState.value
                _uiState.value = if (isRefresh && existing is LeaderboardUiState.Success) {
                    existing.copy(isRefreshing = false)
                } else {
                    LeaderboardUiState.Error(result.error.message)
                }
            }
            Result.Loading -> Unit // Already set above.
        }
    }

    /** Reads the current uid once. Falls back to an empty string when signed out. */
    private suspend fun currentUid(): String =
        sessionRepository.currentUidFlow.first() ?: ""

    @Suppress("unused") // Reserved for future server-side filtering.
    private fun List<LeaderboardEntry>.topThree(): List<LeaderboardEntry> = take(3)
}
