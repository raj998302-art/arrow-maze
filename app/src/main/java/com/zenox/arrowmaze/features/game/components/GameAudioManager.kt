package com.zenox.arrowmaze.features.game.components

import com.zenox.arrowmaze.core.audio.AudioManager
import com.zenox.arrowmaze.core.audio.MusicTrack
import com.zenox.arrowmaze.core.audio.Sfx
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Audio feedback façade for the Game screen.
 *
 * Mirrors [HapticManager] but for sound effects (button taps, arrow rotates,
 * win fanfare, lose buzz). Backed by the real [AudioManager] (Phase 10) which
 * wraps `SoundPool` + `MediaPlayer` — see [RealGameAudioManager].
 *
 * The interface stays minimal so the [GameViewModel] (Phase 4) doesn't need
 * to change when the audio engine lands. Phase 10's `core/audio/AudioManager`
 * exposes a wider surface (background music, more SFX types); this façade
 * only exposes the subset the Game screen needs.
 */
interface GameAudioManager {

    /** Plays the soft "tap" SFX when the player taps an arrow cell. */
    fun playTap()

    /** Plays the rotation SFX when an arrow actually rotates. */
    fun playRotate()

    /** Plays the win fanfare when a level is solved. */
    fun playWin()

    /** Plays the lose buzz when the move cap is hit. */
    fun playLose()

    /** Plays the hint sparkle SFX when a hint is consumed. */
    fun playHint()

    /** Suspends music volume while the win/lose overlay is shown. */
    fun duckMusic()

    /** Restores music volume after the overlay is dismissed. */
    fun restoreMusic()
}

/**
 * No-op stub kept for tests / headless runs. Every call is logged at debug
 * level so callers can verify wiring in logcat.
 */
@Singleton
class StubGameAudioManager @Inject constructor() : GameAudioManager {

    override fun playTap() {
        Timber.d("audio: playTap (stub)")
    }

    override fun playRotate() {
        Timber.d("audio: playRotate (stub)")
    }

    override fun playWin() {
        Timber.d("audio: playWin (stub)")
    }

    override fun playLose() {
        Timber.d("audio: playLose (stub)")
    }

    override fun playHint() {
        Timber.d("audio: playHint (stub)")
    }

    override fun duckMusic() {
        Timber.d("audio: duckMusic (stub)")
    }

    override fun restoreMusic() {
        Timber.d("audio: restoreMusic (stub)")
    }
}

/**
 * Real implementation that delegates to [AudioManager]. Phase 10 binds this
 * as the default [GameAudioManager]; the [StubGameAudioManager] is kept for
 * instrumented tests that don't want to ship audio assets.
 *
 * Mapping:
 * - [playTap] → [Sfx.TAP]
 * - [playRotate] → [Sfx.CORRECT_MOVE] (an arrow rotating is a successful move)
 * - [playWin] → [Sfx.WIN]
 * - [playLose] → [Sfx.LOSE]
 * - [playHint] → [Sfx.HINT]
 * - [duckMusic] → `AudioManager.duckMusic(0.2f)` (drop music to 20% while overlay is up)
 * - [restoreMusic] → `AudioManager.restoreMusic()`
 */
@Singleton
class RealGameAudioManager @Inject constructor(
    private val audioManager: AudioManager,
) : GameAudioManager {

    override fun playTap() {
        audioManager.playSfx(Sfx.TAP)
    }

    override fun playRotate() {
        audioManager.playSfx(Sfx.CORRECT_MOVE)
    }

    override fun playWin() {
        audioManager.playSfx(Sfx.WIN)
    }

    override fun playLose() {
        audioManager.playSfx(Sfx.LOSE)
    }

    override fun playHint() {
        audioManager.playSfx(Sfx.HINT)
    }

    override fun duckMusic() {
        audioManager.duckMusic(OVERLAY_DUCK_FACTOR)
    }

    override fun restoreMusic() {
        audioManager.restoreMusic()
    }

    companion object {
        /** Volume multiplier applied to background music while the win/lose overlay is up. */
        const val OVERLAY_DUCK_FACTOR: Float = 0.2f
    }
}

/**
 * Hilt module that binds [GameAudioManager] to the real [RealGameAudioManager]
 * (Phase 10). The [StubGameAudioManager] is kept around for instrumented tests
 * that want to opt out of audio asset loading.
 *
 * @see MusicTrack for the background-music catalogue (the Game screen doesn't
 *   trigger music playback directly — the NavHost orchestrates that).
 */
@Module
@InstallIn(SingletonComponent::class)
object GameAudioModule {

    @Provides
    @Singleton
    fun provideGameAudioManager(impl: RealGameAudioManager): GameAudioManager = impl
}
