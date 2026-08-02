package com.zenox.arrowmaze.core.designsystem.theme

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.UserSettings
import com.zenox.arrowmaze.core.di.MainImmediateDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of every setting that influences the live [ArrowMazeTheme]
 * composable. The [ThemeManager] holds the latest snapshot so that
 * [MainActivity] can pass it straight into [ArrowMazeTheme] without needing
 * to subscribe to [SettingsRepository] directly.
 */
data class ThemeState(
    val darkMode: ArrowMazeDarkMode = ArrowMazeDarkMode.SYSTEM,
    val themeId: String = "light",
    val highContrast: Boolean = false,
    val colorBlindMode: String = "NONE",
    val fontScale: Float = 1.0f,
)

/** Maps a persisted [UserSettings] row to the live [ThemeState] snapshot. */
fun UserSettings.toThemeState(): ThemeState = ThemeState(
    darkMode = runCatching { ArrowMazeDarkMode.valueOf(darkMode) }
        .getOrDefault(ArrowMazeDarkMode.SYSTEM),
    themeId = themeId,
    highContrast = highContrast,
    colorBlindMode = colorBlindMode,
    fontScale = fontScale,
)

/**
 * App-lifetime bridge between [SettingsRepository] (persistence) and
 * [ArrowMazeTheme] (live composition).
 *
 * - Holds a hot [StateFlow] of [ThemeState] that re-emits whenever the
 *   underlying DataStore writes land.
 * - Each setter (`setDarkMode`, `setTheme`, …) updates the in-memory
 *   snapshot *immediately* so the UI reacts without waiting for the
 *   DataStore round-trip, then persists via the matching
 *   [SettingsRepository] setter.
 *
 * The Settings screen (Phase 9) is the primary writer; everyone else
 * (MainActivity, future Phase 8 shop wiring) reads [themeState].
 */
@Singleton
class ThemeManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @MainImmediateDispatcher private val main: CoroutineDispatcher,
) {
    // CRITICAL: The CoroutineExceptionHandler swallows any uncaught exception
    // from the settings collector so a DataStore IOException (common on first
    // read) doesn't crash the app on the main thread. The app falls back to
    // the default ThemeState() instead.
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t, "ThemeManager: settings flow crashed — using default theme.")
    }
    private val scope = CoroutineScope(SupervisorJob() + main + exceptionHandler)

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    init {
        // Keep the live snapshot in sync with the persisted DataStore values.
        // .catch() prevents a DataStore read failure from crashing the app —
        // the default ThemeState() is kept and the error is logged.
        scope.launch {
            settingsRepository.observe()
                .catch { t ->
                    Timber.e(t, "ThemeManager: settings flow error — keeping default theme.")
                }
                .collect { settings ->
                    _themeState.value = settings.toThemeState()
                }
        }
    }

    /** Updates the in-memory snapshot and persists the new dark-mode value. */
    suspend fun setDarkMode(mode: ArrowMazeDarkMode) {
        _themeState.value = _themeState.value.copy(darkMode = mode)
        persist("setDarkMode") { settingsRepository.setDarkMode(mode.name) }
    }

    /** Updates the in-memory snapshot and persists the new theme id. */
    suspend fun setTheme(themeId: String) {
        _themeState.value = _themeState.value.copy(themeId = themeId)
        persist("setTheme") { settingsRepository.setThemeId(themeId) }
    }

    /** Updates the in-memory snapshot and persists the new high-contrast flag. */
    suspend fun setHighContrast(enabled: Boolean) {
        _themeState.value = _themeState.value.copy(highContrast = enabled)
        persist("setHighContrast") { settingsRepository.setHighContrast(enabled) }
    }

    /** Updates the in-memory snapshot and persists the new color-blind mode. */
    suspend fun setColorBlindMode(mode: String) {
        _themeState.value = _themeState.value.copy(colorBlindMode = mode)
        persist("setColorBlindMode") { settingsRepository.setColorBlindMode(mode) }
    }

    /** Updates the in-memory snapshot and persists the new font scale. */
    suspend fun setFontScale(scale: Float) {
        _themeState.value = _themeState.value.copy(fontScale = scale)
        persist("setFontScale") { settingsRepository.setFontScale(scale) }
    }

    private inline fun persist(tag: String, block: () -> Result<Unit>) {
        val result = block()
        result.onFailure { error ->
            Timber.w(error.asException(), "%s failed", tag)
        }
    }
}
