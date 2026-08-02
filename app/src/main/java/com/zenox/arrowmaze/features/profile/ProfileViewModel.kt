package com.zenox.arrowmaze.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.data.repository.StatsRepository
import com.zenox.arrowmaze.core.domain.model.GameStats
import com.zenox.arrowmaze.core.domain.model.Profile
import com.zenox.arrowmaze.core.firebase.auth.ArrowMazeAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Profile screen.
 *
 * Observes [SessionRepository.currentUidFlow] and flat-maps it onto
 * [ProfileRepository.observeProfile] + [StatsRepository.observeStats] so
 * the screen always reflects the latest signed-in user's profile + stats.
 *
 * Mutation surface:
 *  - [updateDisplayName] / [updateAvatar] / [updateCountry] — re-save the
 *    profile with the targeted field updated.
 *  - [signOut] — calls [ArrowMazeAuth.signOut] + clears the local session.
 *  - [deleteAccount] — calls [ArrowMazeAuth.deleteAccount] + clears the
 *    local session; emits [ProfileNavEvent.AccountDeleted].
 *  - [sendEmailVerification] — fire-and-forget verification email.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository,
    private val auth: ArrowMazeAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<ProfileNavEvent>(capacity = Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    init {
        // Combine current-uid + guest-flag so the flat-map can short-circuit
        // gracefully when the user is signed out.
        viewModelScope.launch {
            sessionRepository.currentUidFlow
                .flatMapLatest { uid ->
                    if (uid == null) {
                        kotlinx.coroutines.flow.flowOf(ProfileUiState.Error("Not signed in"))
                    } else {
                        combine(
                            profileRepository.observeProfile(uid),
                            statsRepository.observeStats(uid),
                        ) { profile, stats ->
                            if (profile == null) {
                                ProfileUiState.Loading
                            } else {
                                ProfileUiState.Success(
                                    profile = profile,
                                    stats = stats ?: GameStats.EMPTY,
                                    isOwnProfile = true,
                                )
                            }
                        }
                    }
                }
                .catch { t ->
                    Timber.e(t, "Profile stream failed")
                    emit(ProfileUiState.Error(t.message ?: "Failed to load profile"))
                }
                .collect { state -> _uiState.value = state }
        }
    }

    /** Updates the user's display name. Best-effort: errors emit a toast. */
    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            viewModelScope.launch { _navEvents.send(ProfileNavEvent.ShowToast("Name too short")) }
            return
        }
        updateProfile { it.copy(displayName = trimmed, playerName = trimmed) }
    }

    /** Updates the user's avatar URL. Pass `null` to clear. */
    fun updateAvatar(url: String?) {
        updateProfile { it.copy(avatarUrl = url?.takeIf(String::isNotBlank)) }
    }

    /** Updates the user's country (ISO-2 code). */
    fun updateCountry(country: String) {
        updateProfile { it.copy(country = country) }
    }

    /** Updates player name (independent of display name). */
    fun updatePlayerName(playerName: String) {
        val trimmed = playerName.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _navEvents.send(ProfileNavEvent.ShowToast("Player name cannot be empty")) }
            return
        }
        updateProfile { it.copy(playerName = trimmed) }
    }

    /** Signs the user out and clears the local session. */
    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
                .onSuccess {
                    sessionRepository.clear()
                    _navEvents.send(ProfileNavEvent.SignedOut)
                }
                .onFailure { error ->
                    Timber.w(error.asException(), "Sign-out failed")
                    _navEvents.send(ProfileNavEvent.ShowToast(error.message))
                }
        }
    }

    /** Permanently deletes the user's account. */
    fun deleteAccount() {
        viewModelScope.launch {
            auth.deleteAccount()
                .onSuccess {
                    sessionRepository.clear()
                    _navEvents.send(ProfileNavEvent.AccountDeleted)
                }
                .onFailure { error ->
                    Timber.w(error.asException(), "Account deletion failed")
                    _navEvents.send(ProfileNavEvent.ShowToast(error.message))
                }
        }
    }

    /** Triggers a verification email for the current user. */
    fun sendEmailVerification() {
        viewModelScope.launch {
            auth.sendEmailVerification()
                .onSuccess {
                    _navEvents.send(ProfileNavEvent.ShowToast("Verification email sent"))
                }
                .onFailure { error ->
                    Timber.w(error.asException(), "Verification email failed")
                    _navEvents.send(ProfileNavEvent.ShowToast(error.message))
                }
        }
    }

    // ---------- internals ----------

    /**
     * Reads the current profile, applies [transform], and writes it back via
     * [ProfileRepository.saveProfile]. Surfaces failures via the toast
     * channel. No-op if there's no profile loaded.
     */
    private fun updateProfile(transform: (Profile) -> Profile) {
        viewModelScope.launch {
            val current = (_uiState.value as? ProfileUiState.Success)?.profile ?: return@launch
            val updated = transform(current)
            profileRepository.saveProfile(updated)
                .onSuccess {
                    _navEvents.send(ProfileNavEvent.ShowToast("Profile updated"))
                }
                .onFailure { error ->
                    Timber.w(error.asException(), "Profile update failed")
                    _navEvents.send(ProfileNavEvent.ShowToast(error.message))
                }
        }
    }
}
