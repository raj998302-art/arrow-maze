package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.UserSettings
import com.zenox.arrowmaze.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete [SettingsRepository]. Aggregates the 10 underlying settings keys
 * into a single [UserSettings] Flow so callers don't need to fan out across
 * 10 separate Flows.
 *
 * All writes go straight to [SettingsDataStore]; no Room involvement.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {

    override fun observe(): Flow<UserSettings> = combine(
        combine(
            dataStore.darkModeFlow,
            dataStore.themeIdFlow,
            dataStore.musicVolumeFlow,
            dataStore.sfxVolumeFlow,
            dataStore.vibrationEnabledFlow,
        ) { darkMode, themeId, music, sfx, vibration ->
            SettingsPartialA(darkMode, themeId, music, sfx, vibration)
        },
        combine(
            dataStore.highContrastFlow,
            dataStore.colorBlindModeFlow,
            dataStore.fontScaleFlow,
            dataStore.notificationsEnabledFlow,
            dataStore.hasSeenOnboardingFlow,
        ) { highContrast, colorBlind, fontScale, notifications, onboarding ->
            SettingsPartialB(highContrast, colorBlind, fontScale, notifications, onboarding)
        },
    ) { a, b ->
        UserSettings(
            darkMode = a.darkMode,
            themeId = a.themeId,
            musicVolume = a.musicVolume,
            sfxVolume = a.sfxVolume,
            vibrationEnabled = a.vibration,
            highContrast = b.highContrast,
            colorBlindMode = b.colorBlind,
            fontScale = b.fontScale,
            notificationsEnabled = b.notifications,
            hasSeenOnboarding = b.onboarding,
        )
    }

    private data class SettingsPartialA(
        val darkMode: String,
        val themeId: String,
        val musicVolume: Int,
        val sfxVolume: Int,
        val vibration: Boolean,
    )

    private data class SettingsPartialB(
        val highContrast: Boolean,
        val colorBlind: String,
        val fontScale: Float,
        val notifications: Boolean,
        val onboarding: Boolean,
    )

    override suspend fun setDarkMode(value: String): Result<Unit> = withContext(io) {
        resultOf { dataStore.setDarkMode(value) }
    }

    override suspend fun setThemeId(value: String): Result<Unit> = withContext(io) {
        resultOf { dataStore.setThemeId(value) }
    }

    override suspend fun setMusicVolume(value: Int): Result<Unit> = withContext(io) {
        resultOf { dataStore.setMusicVolume(value) }
    }

    override suspend fun setSfxVolume(value: Int): Result<Unit> = withContext(io) {
        resultOf { dataStore.setSfxVolume(value) }
    }

    override suspend fun setVibrationEnabled(value: Boolean): Result<Unit> = withContext(io) {
        resultOf { dataStore.setVibrationEnabled(value) }
    }

    override suspend fun setHighContrast(value: Boolean): Result<Unit> = withContext(io) {
        resultOf { dataStore.setHighContrast(value) }
    }

    override suspend fun setColorBlindMode(value: String): Result<Unit> = withContext(io) {
        resultOf { dataStore.setColorBlindMode(value) }
    }

    override suspend fun setFontScale(value: Float): Result<Unit> = withContext(io) {
        resultOf { dataStore.setFontScale(value) }
    }

    override suspend fun setNotificationsEnabled(value: Boolean): Result<Unit> = withContext(io) {
        resultOf { dataStore.setNotificationsEnabled(value) }
    }

    override suspend fun setHasSeenOnboarding(value: Boolean): Result<Unit> = withContext(io) {
        resultOf { dataStore.setHasSeenOnboarding(value) }
    }
}
