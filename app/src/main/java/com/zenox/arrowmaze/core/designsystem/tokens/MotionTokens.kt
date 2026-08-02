package com.zenox.arrowmaze.core.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Motion tokens for Arrow Maze.
 *
 * Centralises durations and easing curves so every animation across the
 * feature screens (Phases 6–9) feels consistent. Values follow Material 3
 * motion guidance (emphasized = standard + slight overshoot for entrance).
 */
object MotionTokens {

    // ---- Durations (ms) ----
    const val DurationInstant = 1
    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 600
    const val DurationLong4 = 800

    // Convenience aliases requested by the spec
    const val DurationShort = DurationShort3      // 150 ms
    const val DurationMedium = DurationMedium2    // 300 ms
    const val DurationLong = DurationLong2        // 500 ms

    // ---- Easings ----

    /** Standard easing for most UI motion (decelerate-in, accelerate-out). */
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Emphasized easing — default for prominent transitions. */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Emphasized decelerate — for entering elements. */
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Emphasized accelerate — for exiting elements. */
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** Generic entrance easing (decelerating). */
    val EnterEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Generic exit easing (accelerating). */
    val ExitEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Spring-like overshoot for celebratory UI (win dialog, coin pops). */
    val OvershootEasing: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    /**
     * Modal "popIn" easing — matches the HTML reference's
     * `cubic-bezier(.2,1.4,.4,1)` keyframe used for `.modal` entrance.
     * Slight overshoot gives dialogs a tactile scale-in feel.
     */
    val PopInEasing: Easing = CubicBezierEasing(0.2f, 1.4f, 0.4f, 1.0f)

    /**
     * Toast easing — matches the HTML reference's
     * `cubic-bezier(.2,1.3,.4,1)` transition on `#toast.show`. Used for
     * slide-in-from-top notifications and one-shot banners.
     */
    val ToastEasing: Easing = CubicBezierEasing(0.2f, 1.3f, 0.4f, 1.0f)

    /**
     * Button-press easing — matches the HTML `.btn:active { transition:
     * transform .12s ease }` rule. Used for the 0.95-scale tap feedback
     * on `ArrowMazeButton.Primary`.
     */
    val PressEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Logo floaty duration — matches HTML `floaty 3.2s ease-in-out infinite`. */
    const val LogoFloatyDurationMs: Int = 3_200

    /**
     * Screen entrance duration — matches HTML `screenIn .35s ease`.
     * Used by NavHost screen transitions.
     */
    const val ScreenEnterDurationMs: Int = 350

    /** Screen entrance easing — decelerate, matches HTML `screenIn .35s ease`. */
    val ScreenEnterEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Linear — used for indeterminate infinite animations. Aliases the
     *  Compose `LinearEasing` so callers can stay within `MotionTokens.*`. */
    val LinearMotion: Easing = LinearEasing

    /** Legacy alias kept for screens that import LinearOutSlowInEasing-style curves. */
    val LegacyLinearOutSlowInEasing: Easing = LinearOutSlowInEasing
}

