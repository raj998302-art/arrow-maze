package com.zenox.arrowmaze.features.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.di.MainDispatcher
import com.zenox.arrowmaze.core.firebase.auth.ArrowMazeAuth
import com.zenox.arrowmaze.core.firebase.auth.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Auth screen.
 *
 * Owns:
 *  - the [AuthUiState] lifecycle (Idle → Loading → Authenticated | Error),
 *  - a one-shot [AuthNavEvent] channel used to signal navigation toasts /
 *    the `onAuthenticated()` callback,
 *  - a hot subscription to [ArrowMazeAuth.currentUser] that auto-navigates
 *    to Home when the session is already authenticated AND the user has
 *    completed the auth flow on a previous launch.
 *
 * Form validation lives in the Composable layer ([AuthScreen]); this VM
 * only fires the auth calls and translates the [Result]s.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: ArrowMazeAuth,
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    @MainDispatcher private val main: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<AuthNavEvent>(capacity = Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /** Reactive snapshot of the current Firebase user. */
    val currentUser: StateFlow<AuthUser?> = auth.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    init {
        // Auto-navigate when a Firebase user is already present AND the user
        // has completed the auth flow on a previous launch (cold-start
        // restoration). The `hasCompletedAuth` gate prevents auto-nav from
        // firing immediately when the user explicitly lands on the Auth screen.
        // .catch() prevents a DataStore read failure from crashing the app.
        viewModelScope.launch {
            sessionRepository.hasCompletedAuthFlow
                .catch { t ->
                    Timber.e(t, "AuthViewModel: hasCompletedAuthFlow read failed — staying on Auth screen.")
                }
                .collect { hasCompleted ->
                    val user = currentUser.value
                    if (hasCompleted && user != null) {
                        Timber.d("Auto-nav: hasCompletedAuth=true & user=%s", user.uid)
                        _navEvents.send(AuthNavEvent.NavigateHome)
                    }
                }
        }
    }

    /** Sign in with an email + password pair. */
    fun signInWithEmail(email: String, password: String) {
        launchAuth {
            auth.signInWithEmail(email, password)
        }
    }

    /** Create a new email/password account. */
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        launchAuth {
            auth.signUpWithEmail(email, password, displayName)
        }
    }

    /** Sign in with a Google ID token returned by the Credential Manager. */
    fun signInWithGoogle(idToken: String) {
        launchAuth {
            auth.signInWithGoogle(idToken)
        }
    }

    /** Create / sign in an anonymous guest account. */
    fun signInAsGuest() {
        launchAuth {
            auth.signInAsGuest()
        }
    }

    /**
     * Send a password-reset email. Does NOT flip the UI to Loading (it's a
     * fire-and-forget side-channel), but emits a [AuthNavEvent.ShowToast]
     * with the outcome.
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            auth.sendPasswordReset(email)
                .onSuccess {
                    _navEvents.send(AuthNavEvent.ShowToast("Password reset email sent"))
                }
                .onFailure { error ->
                    Timber.w(error.asException(), "Password reset failed")
                    _navEvents.send(AuthNavEvent.ShowToast(error.message))
                }
        }
    }

    // ---------- internals ----------

    /**
     * Common wrapper for every auth action: flips the UI to Loading, invokes
     * [block] (which must return a [Result] of [AuthUser]), persists the
     * session state on success, and emits [AuthNavEvent.NavigateHome].
     */
    private fun launchAuth(block: suspend () -> Result<AuthUser>) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading("Authenticating…")
            val result = block()
            result
                .onSuccess { user ->
                    persistSession(user)
                    _uiState.value = AuthUiState.Authenticated(user.uid)
                    _navEvents.send(AuthNavEvent.NavigateHome)
                }
                .onFailure { error ->
                    Timber.w(error.asException(), "Auth failed")
                    _uiState.value = AuthUiState.Error(error.message)
                }
        }
    }

    /**
     * Persists the freshly-authenticated user's uid + isGuest flag to the
     * [SessionRepository] and marks `hasCompletedAuth = true` so the
     * auto-nav logic can fire on subsequent cold starts.
     */
    private suspend fun persistSession(user: AuthUser) {
        sessionRepository.setCurrentUid(user.uid)
        sessionRepository.setIsGuest(user.isAnonymous)
        sessionRepository.setHasCompletedAuth(true)

        // If this is a guest sign-in, also store the guest uid in the
        // progress data store so the merge logic in FirebaseAuthImpl can
        // find it on a subsequent email/Google sign-in.
        if (user.isAnonymous) {
            sessionRepository.setGuestUid(user.uid)
        }
    }
}
