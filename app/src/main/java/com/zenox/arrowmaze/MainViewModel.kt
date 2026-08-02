package com.zenox.arrowmaze

import androidx.lifecycle.ViewModel
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeDarkMode
import com.zenox.arrowmaze.core.designsystem.theme.ThemeManager
import com.zenox.arrowmaze.core.designsystem.theme.ThemeState
import com.zenox.arrowmaze.core.domain.model.GameTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/**
 * Trivial [ViewModel] exposed to [MainActivity] so it can collect the
 * [ThemeManager]'s theme state with the same lifecycle semantics as feature
 * screens.
 *
 * The VM derives a resolved [GameTheme] from the persisted `themeId` (via
 * [GameTheme.byId]) so [MainActivity] can pass a complete [GameTheme] into
 * [com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeTheme] for the
 * cosmetic-theme override.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val themeManager: ThemeManager,
) : ViewModel() {

    /** Hot stream of the full theme-state snapshot (darkMode + themeId + flags). */
    val themeState: StateFlow<ThemeState> = themeManager.themeState

    /** Hot stream of the resolved [GameTheme] (looked up from [ThemeState.themeId]). */
    val currentTheme: StateFlow<GameTheme> = themeManager.themeState
        .map { state -> GameTheme.byId(state.themeId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GameTheme.byId("light"),
        )

    /** Hot stream of the user's dark-mode preference. */
    val darkMode: StateFlow<ArrowMazeDarkMode> = themeManager.themeState
        .map { it.darkMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ArrowMazeDarkMode.SYSTEM,
        )

    /** Hot stream of the high-contrast accessibility flag. */
    val highContrast: StateFlow<Boolean> = themeManager.themeState
        .map { it.highContrast }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )
}
