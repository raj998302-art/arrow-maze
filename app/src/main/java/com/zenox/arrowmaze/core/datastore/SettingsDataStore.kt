package com.zenox.arrowmaze.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferences-backed settings store. One row per user setting, all of which
 * are read via the SettingsRepository's `observe()` and updated via `update*`.
 *
 * Defaults are applied at the Flow level so that a fresh install still emits
 * sensible values without an explicit write.
 */
class SettingsDataStore(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")           // SYSTEM / LIGHT / DARK
        val THEME_ID = stringPreferencesKey("theme_id")
        val MUSIC_VOLUME = intPreferencesKey("music_volume")
        val SFX_VOLUME = intPreferencesKey("sfx_volume")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val COLOR_BLIND_MODE = stringPreferencesKey("color_blind_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    // ---------- Getters (Flow) ----------

    val darkModeFlow: Flow<String> = dataStore.data.map { it[Keys.DARK_MODE] ?: DEFAULT_DARK_MODE }
    val themeIdFlow: Flow<String> = dataStore.data.map { it[Keys.THEME_ID] ?: DEFAULT_THEME_ID }
    val musicVolumeFlow: Flow<Int> = dataStore.data.map { it[Keys.MUSIC_VOLUME] ?: DEFAULT_MUSIC_VOLUME }
    val sfxVolumeFlow: Flow<Int> = dataStore.data.map { it[Keys.SFX_VOLUME] ?: DEFAULT_SFX_VOLUME }
    val vibrationEnabledFlow: Flow<Boolean> = dataStore.data.map { it[Keys.VIBRATION_ENABLED] ?: DEFAULT_VIBRATION_ENABLED }
    val highContrastFlow: Flow<Boolean> = dataStore.data.map { it[Keys.HIGH_CONTRAST] ?: DEFAULT_HIGH_CONTRAST }
    val colorBlindModeFlow: Flow<String> = dataStore.data.map { it[Keys.COLOR_BLIND_MODE] ?: DEFAULT_COLOR_BLIND_MODE }
    val fontScaleFlow: Flow<Float> = dataStore.data.map { it[Keys.FONT_SCALE] ?: DEFAULT_FONT_SCALE }
    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: DEFAULT_NOTIFICATIONS_ENABLED }
    val hasSeenOnboardingFlow: Flow<Boolean> = dataStore.data.map { it[Keys.HAS_SEEN_ONBOARDING] ?: DEFAULT_HAS_SEEN_ONBOARDING }

    // ---------- Setters (suspend) ----------

    suspend fun setDarkMode(value: String) {
        dataStore.edit { it[Keys.DARK_MODE] = value }
    }

    suspend fun setThemeId(value: String) {
        dataStore.edit { it[Keys.THEME_ID] = value }
    }

    suspend fun setMusicVolume(value: Int) {
        dataStore.edit { it[Keys.MUSIC_VOLUME] = value.coerceIn(0, 100) }
    }

    suspend fun setSfxVolume(value: Int) {
        dataStore.edit { it[Keys.SFX_VOLUME] = value.coerceIn(0, 100) }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        dataStore.edit { it[Keys.VIBRATION_ENABLED] = value }
    }

    suspend fun setHighContrast(value: Boolean) {
        dataStore.edit { it[Keys.HIGH_CONTRAST] = value }
    }

    suspend fun setColorBlindMode(value: String) {
        dataStore.edit { it[Keys.COLOR_BLIND_MODE] = value }
    }

    suspend fun setFontScale(value: Float) {
        dataStore.edit { it[Keys.FONT_SCALE] = value.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = value }
    }

    companion object {
        const val DEFAULT_DARK_MODE = "SYSTEM"
        const val DEFAULT_THEME_ID = "light"
        const val DEFAULT_MUSIC_VOLUME = 70
        const val DEFAULT_SFX_VOLUME = 90
        const val DEFAULT_VIBRATION_ENABLED = true
        const val DEFAULT_HIGH_CONTRAST = false
        const val DEFAULT_COLOR_BLIND_MODE = "NONE"
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
        const val DEFAULT_HAS_SEEN_ONBOARDING = false
    }
}
