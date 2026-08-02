package com.zenox.arrowmaze.features.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.WinDialog
import com.zenox.arrowmaze.core.designsystem.tokens.MotionTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.game.GameUiState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen celebratory overlay shown when the player solves a level.
 *
 * Composition
 * -----------
 *  - A semi-transparent dimming scrim so the solved board is still visible
 *    behind the overlay.
 *  - A confetti `Canvas` with [ConfettiLayer] — `N` particles, each with a
 *    random initial angle / speed / colour, animated via a single
 *    `rememberInfiniteTransition` so all particles stay in sync.
 *  - A centred [WinDialog] showing the moves / time / coins stats with
 *    "Continue" + "Replay" actions.
 *
 * The overlay enters with `fadeIn + scaleIn` over [MotionTokens.DurationLong]
 * using [MotionTokens.OvershootEasing] for a slight spring-pop.
 *
 * @param state       The terminal [GameUiState.Won] payload.
 * @param onContinue  Fired when the player taps "Continue" (navigates to the
 *                     next level — owned by the screen).
 * @param onReplay    Fired when the player taps "Replay" (regenerates the
 *                     same level — owned by the screen).
 */
@Composable
fun WinOverlay(
    state: GameUiState.Won,
    onContinue: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = androidx.compose.material3.MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.EmphasizedDecelerateEasing),
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.OvershootEasing),
        ),
        exit = fadeOut(animationSpec = tween(MotionTokens.DurationMedium)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            cs.primaryContainer.copy(alpha = 0.35f),
                            cs.scrim.copy(alpha = 0.55f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Confetti layer behind the dialog.
            ConfettiLayer(
                modifier = Modifier.fillMaxSize(),
                seed = state.timeMs + state.moves,
            )

            // Reuse the design-system WinDialog for the stats + continue
            // affordance, then layer a "Replay" button beneath it so the
            // spec's two-action surface is preserved.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WinDialog(
                    visible = true,
                    moves = state.moves,
                    timeSeconds = ((state.timeMs / 1000L).toInt()),
                    coinsEarned = state.coinsEarned,
                    onContinue = onContinue,
                )
                Spacer(Modifier.height(SpacingTokens.md))
                ArrowMazeButton(
                    text = "Replay",
                    onClick = onReplay,
                    style = ButtonStyle.Outline,
                    leadingIcon = Icons.Rounded.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Confetti particle layer. Renders [ParticleCount] particles, each
 * deterministic in [seed] so the same level win always produces the same
 * confetti pattern. The particles' progress is driven by a single
 * [rememberInfiniteTransition] for performance.
 *
 * Performance: every particle's position is computed inside the draw lambda
 * from the deterministic seed + animated progress, so no per-frame
 * allocations are made.
 */
@Composable
private fun ConfettiLayer(
    modifier: Modifier = Modifier,
    seed: Long = 0L,
    particleCount: Int = ParticleCount,
) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ConfettiCycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confetti-progress",
    )

    // Per-particle phase offset so the burst looks organic rather than
    // synchronised.
    val phaseOffsets = remember(particleCount, seed) {
        val rng = Random(seed)
        FloatArray(particleCount) { rng.nextFloat() }
    }

    // Particle definitions (angle, radius, color index, size, spin) —
    // generated once and reused across frames.
    val particles = remember(particleCount, seed) {
        val rng = Random(seed)
        buildList {
            repeat(particleCount) { i ->
                add(
                    Particle(
                        angleDeg = rng.nextFloat() * 360f,
                        radiusFraction = 0.20f + rng.nextFloat() * 0.55f,
                        colorIndex = i % ConfettiColors.size,
                        sizePx = (6f + rng.nextFloat() * 10f),
                        spinDegPerCycle = (rng.nextFloat() - 0.5f) * 720f,
                        phase = phaseOffsets[i],
                    ),
                )
            }
        }
    }

    val density = LocalDensity.current
    val canvasSizePx = with(density) { 360.dp.toPx() } // logical size; we draw centred.

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = minOf(size.width, size.height) * 0.45f

        // Each particle's lifecycle: rise + spin + fade. We compute the
        // effective progress including the per-particle phase so the burst
        // looks staggered.
        for (p in particles) {
            val pProgress = ((progress + p.phase) % 1f)
            val r = maxRadius * p.radiusFraction * (0.4f + 0.6f * pProgress)
            val angleRad = Math.toRadians(p.angleDeg.toDouble())
            val px = cx + (r * cos(angleRad)).toFloat()
            val py = cy + (r * sin(angleRad)).toFloat() - (canvasSizePx * 0.18f * pProgress)
            val rotation = p.spinDegPerCycle * pProgress
            val alpha = (1f - pProgress).coerceIn(0f, 1f)
            val color = ConfettiColors[p.colorIndex].copy(alpha = alpha)
            rotate(degrees = rotation, pivot = Offset(px, py)) {
                drawRect(
                    color = color,
                    topLeft = Offset(px - p.sizePx / 2f, py - p.sizePx / 2f),
                    size = androidx.compose.ui.geometry.Size(p.sizePx, p.sizePx * 0.6f),
                )
            }
        }
    }
}

/** One confetti particle. Pure data — see [ConfettiLayer] for rendering. */
private data class Particle(
    val angleDeg: Float,
    val radiusFraction: Float,
    val colorIndex: Int,
    val sizePx: Float,
    val spinDegPerCycle: Float,
    val phase: Float,
)

/** Particle count. Higher = denser burst, lower = better perf on low-end. */
private const val ParticleCount = 36

/** Confetti cycle duration in ms. */
private const val ConfettiCycleMs = 2600

/** Palette used for the confetti pieces — bright, celebratory. */
private val ConfettiColors: List<Color> = listOf(
    Color(0xFFFFC107), // gold
    Color(0xFF7B4DFF), // violet
    Color(0xFF3B6CFF), // blue
    Color(0xFF00C853), // green
    Color(0xFFFF5252), // red
    Color(0xFFFFD54F), // light gold
)

/**
 * Reserved draw-scope helper kept so the file self-documents the particle
 * rendering pipeline. Currently a no-op — see [ConfettiLayer] for the real
 * implementation.
 */
@Suppress("unused")
private fun DrawScope.drawParticle() {
    // Intentionally empty; see ConfettiLayer Canvas body.
}
