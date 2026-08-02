package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level settings model. Mirrors [SettingsDataStore]'s 10 keys.
 *
 * The repository wraps the data store and exposes this single object so
 * that callers don't need to fan out across 10 Flows.
 */
data class UserSettings(
    val darkMode: String,            // SYSTEM / LIGHT / DARK
    val themeId: String,
    val musicVolume: Int,            // 0..100
    val sfxVolume: Int,              // 0..100
    val vibrationEnabled: Boolean,
    val highContrast: Boolean,
    val colorBlindMode: String,      // NONE / PROTANOPIA / DEUTERANOPIA / TRITANOPIA
    val fontScale: Float,            // 0.5..2.0
    val notificationsEnabled: Boolean,
    val hasSeenOnboarding: Boolean,
)

/**
 * Settings repository. Wraps [com.zenox.arrowmaze.core.datastore.SettingsDataStore]
 * and exposes a single [UserSettings] Flow plus targeted update methods.
 */
interface SettingsRepository {

    /** Reactive stream of the full settings object. */
    fun observe(): Flow<UserSettings>

    suspend fun setDarkMode(value: String): Result<Unit>
    suspend fun setThemeId(value: String): Result<Unit>
    suspend fun setMusicVolume(value: Int): Result<Unit>
    suspend fun setSfxVolume(value: Int): Result<Unit>
    suspend fun setVibrationEnabled(value: Boolean): Result<Unit>
    suspend fun setHighContrast(value: Boolean): Result<Unit>
    suspend fun setColorBlindMode(value: String): Result<Unit>
    suspend fun setFontScale(value: Float): Result<Unit>
    suspend fun setNotificationsEnabled(value: Boolean): Result<Unit>
    suspend fun setHasSeenOnboarding(value: Boolean): Result<Unit>
}
