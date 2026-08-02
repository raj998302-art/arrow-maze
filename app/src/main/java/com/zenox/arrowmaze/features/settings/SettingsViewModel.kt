package com.zenox.arrowmaze.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.UserSettings
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeDarkMode
import com.zenox.arrowmaze.core.designsystem.theme.ThemeManager
import com.zenox.arrowmaze.core.firebase.auth.ArrowMazeAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Settings screen.
 *
 * Persists every preference immediately via [SettingsRepository] and also
 * forwards appearance-affecting changes (dark mode, theme, high-contrast,
 * color-blind, font scale) to [ThemeManager] so the live [ArrowMazeTheme]
 * re-emits without waiting for the DataStore round-trip.
 *
 * Account actions (`sendEmailVerification`, `signOut`, `deleteAccount`) are
 * funneled through [ArrowMazeAuth]; results surface as one-shot
 * [SettingsUiEvent]s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val themeManager: ThemeManager,
    private val auth: ArrowMazeAuth,
) : ViewModel() {

    private val _uiEvents = Channel<SettingsUiEvent>(capacity = Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observe(),
        auth.currentUser,
    ) { settings, user ->
        // Explicitly typed so the `.catch` block can emit the Error subtype.
        val state: SettingsUiState = SettingsUiState.Success(settings = settings, authUser = user)
        state
    }
        .catch { t ->
            Timber.e(t, "Settings stream failed")
            emit(SettingsUiState.Error(t.message ?: "Failed to load settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Loading,
        )

    // ---------- Appearance ----------

    fun setDarkMode(mode: ArrowMazeDarkMode) {
        viewModelScope.launch {
            themeManager.setDarkMode(mode)
            settingsRepository.setDarkMode(mode.name)
                .onFailure { error ->
                    Timber.w(error.asException(), "setDarkMode persistence failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun setTheme(themeId: String) {
        viewModelScope.launch {
            themeManager.setTheme(themeId)
            settingsRepository.setThemeId(themeId)
                .onFailure { error ->
                    Timber.w(error.asException(), "setTheme persistence failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setHighContrast(enabled)
            settingsRepository.setHighContrast(enabled)
                .onFailure { error ->
                    Timber.w(error.asException(), "setHighContrast persistence failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun setColorBlindMode(mode: String) {
        viewModelScope.launch {
            themeManager.setColorBlindMode(mode)
            settingsRepository.setColorBlindMode(mode)
                .onFailure { error ->
                    Timber.w(error.asException(), "setColorBlindMode persistence failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            themeManager.setFontScale(scale)
            settingsRepository.setFontScale(scale)
                .onFailure { error ->
                    Timber.w(error.asException(), "setFontScale persistence failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    // ---------- Audio ----------

    fun setMusicVolume(value: Int) {
        viewModelScope.launch {
            settingsRepository.setMusicVolume(value)
                .onFailure { error ->
                    Timber.w(error.asException(), "setMusicVolume failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun setSfxVolume(value: Int) {
        viewModelScope.launch {
            settingsRepository.setSfxVolume(value)
                .onFailure { error ->
                    Timber.w(error.asException(), "setSfxVolume failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
                .onFailure { error ->
                    Timber.w(error.asException(), "setVibration failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    // ---------- Notifications ----------

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
                .onFailure { error ->
                    Timber.w(error.asException(), "setNotifications failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    // ---------- Account ----------

    fun sendEmailVerification() {
        viewModelScope.launch {
            auth.sendEmailVerification()
                .onSuccess { _uiEvents.send(SettingsUiEvent.ShowToast("Verification email sent")) }
                .onFailure { error ->
                    Timber.w(error.asException(), "sendEmailVerification failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
                .onSuccess { _uiEvents.send(SettingsUiEvent.SignedOut) }
                .onFailure { error ->
                    Timber.w(error.asException(), "signOut failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            auth.deleteAccount()
                .onSuccess { _uiEvents.send(SettingsUiEvent.AccountDeleted) }
                .onFailure { error ->
                    Timber.w(error.asException(), "deleteAccount failed")
                    _uiEvents.send(SettingsUiEvent.ShowToast(error.message))
                }
        }
    }

    /** Convenience accessor used by the screen for one-shot reads. */
    @Suppress("unused")
    fun currentSettingsOrNull(): UserSettings? =
        (uiState.value as? SettingsUiState.Success)?.settings
}
