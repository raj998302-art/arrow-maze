package com.zenox.arrowmaze.core.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.UserSettings
import com.zenox.arrowmaze.core.di.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Background-music tracks. Each value maps to a raw resource name
 * `raw/music_<name>` (e.g. `raw/music_menu`, `raw/music_game_easy`).
 *
 * If the resource is missing, [AudioManager.playMusic] silently no-ops so
 * the build stays green without shipping audio assets.
 */
enum class MusicTrack(val rawResName: String) {
    MENU("music_menu"),
    GAME_EASY("music_game_easy"),
    GAME_NORMAL("music_game_normal"),
    GAME_HARD("music_game_hard"),
    DAILY("music_daily"),
}

/**
 * Sound effects. Each value maps to a raw resource name `raw/sfx_<name>`
 * (e.g. `raw/sfx_tap`, `raw/sfx_win`).
 *
 * If the resource is missing, [AudioManager.playSfx] silently no-ops.
 */
enum class Sfx(val rawResName: String) {
    TAP("sfx_tap"),
    WRONG_MOVE("sfx_wrong_move"),
    CORRECT_MOVE("sfx_correct_move"),
    WIN("sfx_win"),
    LOSE("sfx_lose"),
    HINT("sfx_hint"),
    PURCHASE("sfx_purchase"),
    ACHIEVEMENT("sfx_achievement"),
    NAVIGATION("sfx_navigation"),
}

/**
 * Central audio engine for Arrow Maze. Plays background music (looping
 * [MediaPlayer]) and one-shot sound effects ([SoundPool] with `USAGE_GAME`).
 *
 * Reactively observes [SettingsRepository.observe] — when the user changes
 * the music / SFX volume sliders in Settings, the engine re-applies the
 * new volume to the running [MediaPlayer] / scales future [SoundPool] plays.
 * When music volume is set to 0, music is paused (not stopped — so resuming
 * is instant).
 *
 * Asset-graceful: every method that loads a raw resource is wrapped in
 * try-catch so the build compiles even when no `res/raw` files exist
 * yet. The orchestrator only needs to ship raw assets to enable audio.
 *
 * Lifecycle: call [release] from `Application.onTerminate()` or a process-
 * death lifecycle observer to free the [MediaPlayer] + [SoundPool].
 */
@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val settingsRepo: SettingsRepository,
    @MainDispatcher private val main: CoroutineDispatcher,
) {

    /** Background-music player — lazily created on first [playMusic]. */
    @Volatile private var musicPlayer: MediaPlayer? = null

    /**
     * SFX player — created lazily on first [playSfx] / [playMusic] call so
     * the AudioManager construction never blocks the main thread on
     * SoundPool's native init. The `PUBLICATION` mode is safe because every
     * call site synchronises on `this` (the singleton).
     */
    private val soundPool: SoundPool by lazy(PUBLICATION) {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    /** Cache of `Sfx → soundId` returned by `soundPool.load`. */
    private val sfxIds = mutableMapOf<Sfx, Int>()

    /** Cache of `Sfx → Boolean` for whether the resource exists at all. */
    private val sfxAvailable = mutableMapOf<Sfx, Boolean>()

    /** Currently-playing music track (so resume can pick the right resource). */
    @Volatile private var currentTrack: MusicTrack? = null

    /** Current music volume (0..100) — applied to the running MediaPlayer. */
    @Volatile private var musicVolume: Int = DEFAULT_MUSIC_VOLUME

    /** Current SFX volume (0..100) — passed to every `soundPool.play`. */
    @Volatile private var sfxVolume: Int = DEFAULT_SFX_VOLUME

    /** "Ducked" multiplier applied when an overlay mutes music temporarily. */
    @Volatile private var musicDuckFactor: Float = 1f

    /** Background scope for the settings-flow collector. */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + main)

    /** Active settings-flow collector — cancelled in [release]. */
    private var settingsJob: Job? = null

    /** True once [start] has been called — guards against re-entrancy. */
    @Volatile private var started: Boolean = false

    init {
        // Pre-touch the SFX availability cache so playSfx doesn't hit
        // resources.getIdentifier on the audio thread. This is a cheap
        // resources lookup and does NOT allocate the SoundPool.
        Sfx.entries.forEach { sfx ->
            sfxAvailable[sfx] = resolveRawId(sfx.rawResName) != 0
        }
        // NOTE: observeSettings() + SoundPool creation are deferred to
        // [start] so the singleton constructor stays allocation-free.
        // Callers (e.g. ArrowMazeApplication) should invoke [start] on app
        // launch; the first [playMusic] / [playSfx] call also triggers it
        // defensively.
    }

    /**
     * Boots the reactive settings subscription. Safe to call multiple times
     * — the second call is a no-op. Idempotent so callers don't need to
     * coordinate.
     */
    fun start() {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            observeSettings()
        }
    }

    /**
     * Loads + loops [track]. Re-uses the existing [MediaPlayer] when the
     * track is already current (no-op). Otherwise releases the prior player
     * and creates a new one.
     */
    fun playMusic(track: MusicTrack) {
        ensureStarted()
        if (currentTrack == track && musicPlayer?.isPlaying == true) {
            Timber.d("playMusic: track=$track already playing — no-op.")
            return
        }
        stopMusicInternal()
        val rawId = resolveRawId(track.rawResName)
        if (rawId == 0) {
            Timber.w("playMusic: raw resource ${track.rawResName} not found — no-op.")
            currentTrack = track
            return
        }
        try {
            val player = MediaPlayer.create(context, rawId) ?: run {
                Timber.w("playMusic: MediaPlayer.create returned null for ${track.rawResName}.")
                currentTrack = track
                return
            }
            player.isLooping = true
            applyMusicVolume(player)
            player.start()
            musicPlayer = player
            currentTrack = track
            Timber.i("playMusic: started ${track.rawResName}.")
        } catch (t: Throwable) {
            Timber.w(t, "playMusic: failed to start ${track.rawResName}.")
        }
    }

    /** Stops the music + releases the underlying [MediaPlayer]. */
    fun stopMusic() {
        stopMusicInternal()
        currentTrack = null
    }

    /** Pauses the music (resume is instant — keeps the [MediaPlayer] alive). */
    fun pauseMusic() {
        val p = musicPlayer ?: return
        try {
            if (p.isPlaying) {
                p.pause()
                Timber.d("pauseMusic: paused ${currentTrack?.rawResName}.")
            }
        } catch (t: Throwable) {
            Timber.w(t, "pauseMusic: failed.")
        }
    }

    /** Resumes music after [pauseMusic] (no-op if music is already playing). */
    fun resumeMusic() {
        val p = musicPlayer ?: return
        if (musicVolume == 0) {
            Timber.d("resumeMusic: skipping — musicVolume=0.")
            return
        }
        try {
            if (!p.isPlaying) {
                p.start()
                Timber.d("resumeMusic: resumed ${currentTrack?.rawResName}.")
            }
        } catch (t: Throwable) {
            Timber.w(t, "resumeMusic: failed.")
        }
    }

    /**
     * Plays [sfx] once. Loads the underlying raw resource lazily on first
     * use (cached in [sfxIds]). No-ops if the resource is missing or the
     * sound is still loading.
     */
    fun playSfx(sfx: Sfx) {
        ensureStarted()
        if (sfxVolume == 0) {
            Timber.d("playSfx: skipping $sfx — sfxVolume=0.")
            return
        }
        if (sfxAvailable[sfx] != true) {
            Timber.d("playSfx: skipping $sfx — resource not available.")
            return
        }
        val soundId = sfxIds.getOrPut(sfx) {
            val rawId = resolveRawId(sfx.rawResName)
            if (rawId == 0) {
                sfxAvailable[sfx] = false
                return@getOrPut 0
            }
            soundPool.load(context, rawId, 1)
        }
        if (soundId == 0) return
        val vol = sfxVolume / 100f
        soundPool.play(soundId, vol, vol, 1, 0, 1f)
    }

    /** Sets the music volume (0..100) live. */
    fun setMusicVolume(v: Int) {
        musicVolume = v.coerceIn(0, 100)
        val p = musicPlayer ?: return
        applyMusicVolume(p)
        if (musicVolume == 0) {
            pauseMusic()
        } else {
            resumeMusic()
        }
    }

    /** Sets the SFX volume (0..100) — applied to future [playSfx] calls. */
    fun setSfxVolume(v: Int) {
        sfxVolume = v.coerceIn(0, 100)
    }

    /**
     * Ducks the music volume (e.g. when the win/lose overlay is shown).
     * Pass `1f` to restore.
     */
    fun duckMusic(factor: Float) {
        musicDuckFactor = factor.coerceIn(0f, 1f)
        musicPlayer?.let { applyMusicVolume(it) }
    }

    /** Restores music volume to its non-ducked level. */
    fun restoreMusic() {
        duckMusic(1f)
    }

    /** Releases the underlying [MediaPlayer] + [SoundPool]. Idempotent. */
    fun release() {
        Timber.i("AudioManager.release()")
        settingsJob?.cancel()
        settingsJob = null
        stopMusicInternal()
        currentTrack = null
        // Only release the SoundPool if it was actually created (lazy).
        if (started) {
            try {
                soundPool.release()
            } catch (t: Throwable) {
                Timber.w(t, "SoundPool.release failed.")
            }
        }
        scope.cancel()
        started = false
    }

    /**
     * Defensive: boot the settings subscription on first audio call so
     * callers that don't explicitly invoke [start] still get reactive
     * volume updates. Idempotent.
     */
    private fun ensureStarted() {
        if (!started) start()
    }

    // ---- Internals ----

    private fun stopMusicInternal() {
        musicPlayer?.let { p ->
            try {
                if (p.isPlaying) p.stop()
            } catch (t: Throwable) {
                Timber.w(t, "stopMusicInternal: stop() failed.")
            }
            try {
                p.release()
            } catch (t: Throwable) {
                Timber.w(t, "stopMusicInternal: release() failed.")
            }
        }
        musicPlayer = null
    }

    private fun applyMusicVolume(player: MediaPlayer) {
        val vol = (musicVolume / 100f) * musicDuckFactor
        try {
            player.setVolume(vol, vol)
        } catch (t: Throwable) {
            Timber.w(t, "applyMusicVolume failed.")
        }
    }

    private fun resolveRawId(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = settingsRepo.observe()
            .onEach { settings -> applySettings(settings) }
            .launchIn(scope)
    }

    /** Pushes the user's settings into the engine's live volume state. */
    private fun applySettings(settings: UserSettings) {
        Timber.d("applySettings: music=${settings.musicVolume} sfx=${settings.sfxVolume}")
        val musicChanged = settings.musicVolume != musicVolume
        val sfxChanged = settings.sfxVolume != sfxVolume
        if (musicChanged) setMusicVolume(settings.musicVolume)
        if (sfxChanged) sfxVolume = settings.sfxVolume
    }

    companion object {
        const val DEFAULT_MUSIC_VOLUME: Int = 60
        const val DEFAULT_SFX_VOLUME: Int = 80
    }
}
