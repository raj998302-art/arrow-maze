package com.zenox.arrowmaze.features.game.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Cross-platform haptic feedback façade for the Game screen.
 *
 * The Game ViewModel calls these methods at well-defined moments (tap, rotate,
 * success, error) so the implementation can choose the appropriate vibration
 * waveform without leaking platform APIs into the business logic.
 *
 * The default implementation, [VibratorHapticManager], wraps Android's
 * [Vibrator] / [VibratorManager] (API 31+) and is a no-op on devices without
 * a vibrator. Phase 9 will add a settings-driven toggle that disables all
 * methods when `vibrationEnabled = false`.
 */
interface HapticManager {

    /** Short, light tick on every cell tap. */
    fun tap()

    /** Slightly heavier pulse when an arrow actually rotates. */
    fun rotate()

    /** Triumphant double-pulse for a level-completion win. */
    fun success()

    /** Sharp error buzz when the move cap is hit. */
    fun error()
}

/**
 * Singleton implementation backed by the platform [Vibrator].
 *
 * Resolves the vibrator via [VibratorManager] on API 31+ (where direct
 * `Context.VIBRATOR_SERVICE` is deprecated) and falls back to the legacy
 * service on older API levels. All calls are guarded so a missing vibrator
 * (e.g. on tablets / emulator without haptic hardware) silently no-ops.
 *
 * The [enabled] flag is hard-wired to `true` for Phase 6 — the Settings
 * screen (Phase 9) will inject a runtime-toggled wrapper.
 */
@Singleton
class VibratorHapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enabled: Boolean = true,
) : HapticManager {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun tap() {
        if (!enabled) return
        vibrate(durationMs = 12L, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
    }

    override fun rotate() {
        if (!enabled) return
        vibrate(durationMs = 22L, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
    }

    override fun success() {
        if (!enabled) return
        // Two short pulses with a tiny gap — a classic "ta-da" cadence.
        val timings = longArrayOf(0L, 30L, 80L, 60L)
        val amplitudes = intArrayOf(0, 180, 0, 255)
        vibratePattern(timings, amplitudes)
    }

    override fun error() {
        if (!enabled) return
        vibrate(durationMs = 80L, amplitude = 220)
    }

    // ---------- internals ----------

    private fun vibrate(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }.onFailure { Timber.w(it, "Vibrator.oneShot failed") }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        }.onFailure { Timber.w(it, "Vibrator.waveform failed") }
    }
}

/**
 * Hilt module that binds the [HapticManager] interface to its concrete
 * [VibratorHapticManager] implementation as a singleton. Provided separately
 * from the data-layer modules so the feature module owns its own platform
 * integrations.
 */
@Module
@InstallIn(SingletonComponent::class)
object GameHapticsModule {

    @Provides
    @Singleton
    fun provideHapticManager(
        @ApplicationContext context: Context,
    ): HapticManager = VibratorHapticManager(context = context, enabled = true)
}
