package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.MotionTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import kotlinx.coroutines.delay

/**
 * Branded Material 3 [AlertDialog] wrapper. Applies the Arrow Maze shape
 * (20.dp rounded), brand colour scheme, optional leading icon and the
 * standard confirm / dismiss action pair. Drops in for confirmations,
 * sign-out prompts, settings resets, etc.
 */
@Composable
fun ArrowMazeDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss?.invoke() },
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
            )
        },
        text = message?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = cs.primary)
            }
        },
        dismissButton = if (dismissText != null && onDismiss != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = dismissText, color = cs.onSurfaceVariant)
                }
            }
        } else null,
        shape = RoundedCornerShape(26.dp),
        containerColor = cs.surface,
        iconContentColor = cs.primary,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurfaceVariant,
        tonalElevation = ElevationTokens.Level3,
    )
}

/**
 * Celebratory level-complete dialog. Shows a scaling-in [ArrowMazeIcons.Trophy]
 * inside a soft radial halo, then the level stats (moves, time, coins earned)
 * and a full-width "Continue" button.
 *
 * Uses [AnimatedVisibility] + [scaleIn] with [MotionTokens.OvershootEasing]
 * for the spring-pop entrance on the trophy.
 */
@Composable
fun WinDialog(
    visible: Boolean,
    moves: Int,
    timeSeconds: Int,
    coinsEarned: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val cs = MaterialTheme.colorScheme

    // Drive the scale-in: false on first composition, flip to true after a
    // tiny delay so the AnimatedVisibility actually animates.
    var iconVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        iconVisible = true
    }

    // Modal popIn: scale 0.8 → 1.0 with HTML's exact cubic-bezier(.2,1.4,.4,1)
    // over 320ms. Matches the HTML `.modal { animation: popIn .32s
    // cubic-bezier(.2,1.4,.4,1) }` keyframe.
    var dialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Single-frame delay so animateFloatAsState picks up the transition.
        dialogVisible = true
    }
    val popInScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = POP_IN_DURATION_MS,
            easing = MotionTokens.PopInEasing,
        ),
        label = "win-pop-in",
    )
    val popInAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.DurationMedium,
            easing = MotionTokens.EmphasizedDecelerateEasing,
        ),
        label = "win-pop-in-alpha",
    )

    Dialog(onDismissRequest = onContinue) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .scale(popInScale, popInScale)
                .graphicsLayerAlpha(popInAlpha),
            shape = RoundedCornerShape(26.dp),
            color = cs.surface,
            tonalElevation = ElevationTokens.Level3,
            shadowElevation = ElevationTokens.Level4,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingTokens.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(
                    visible = iconVisible,
                    enter = scaleIn(
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationLong,
                            easing = MotionTokens.OvershootEasing,
                        ),
                    ) + fadeIn(
                        animationSpec = tween(MotionTokens.DurationMedium),
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        cs.primaryContainer,
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = ArrowMazeIcons.Trophy,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(60.dp),
                        )
                    }
                }
                Spacer(Modifier.height(SpacingTokens.lg))
                // Gradient title — matches HTML `.modal h2 { background:
                // linear-gradient(90deg,var(--accent1),var(--accent2));
                // -webkit-background-clip:text;color:transparent }`.
                Text(
                    text = stringResource(R.string.game_win_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet)),
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(SpacingTokens.sm))
                Text(
                    text = "Brilliant — you solved the maze.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(SpacingTokens.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(label = "Moves", value = moves.toString())
                    VerticalDivider()
                    StatItem(label = "Time", value = formatTime(timeSeconds))
                    VerticalDivider()
                    StatItem(label = "Coins", value = "+$coinsEarned", valueColor = cs.tertiary)
                }
                Spacer(Modifier.height(SpacingTokens.xl))
                ArrowMazeButton(
                    text = stringResource(R.string.common_continue),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Primary,
                )
            }
        }
    }
}

/**
 * "Out of moves" lose dialog. Shows a sad-face icon, the level title, a
 * short message, and two actions: [onRetry] (primary) and [onUseHint]
 * (tonal with leading [ArrowMazeIcons.Hint]).
 */
@Composable
fun LoseDialog(
    visible: Boolean,
    onRetry: () -> Unit,
    onUseHint: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "Don't worry — try again, or use a hint to reveal the next move.",
    canUseHint: Boolean = true,
) {
    if (!visible) return
    val cs = MaterialTheme.colorScheme

    // Modal popIn — matches HTML `.modal { animation: popIn .32s
    // cubic-bezier(.2,1.4,.4,1) }`.
    var dialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dialogVisible = true }
    val popInScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = POP_IN_DURATION_MS,
            easing = MotionTokens.PopInEasing,
        ),
        label = "lose-pop-in",
    )
    val popInAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.DurationMedium,
            easing = MotionTokens.EmphasizedDecelerateEasing,
        ),
        label = "lose-pop-in-alpha",
    )

    Dialog(onDismissRequest = onRetry) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .scale(popInScale, popInScale)
                .graphicsLayerAlpha(popInAlpha),
            shape = RoundedCornerShape(26.dp),
            color = cs.surface,
            tonalElevation = ElevationTokens.Level3,
            shadowElevation = ElevationTokens.Level4,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingTokens.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = cs.errorContainer,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SentimentDissatisfied,
                        contentDescription = null,
                        tint = cs.onErrorContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.height(SpacingTokens.lg))
                // Gradient title — matches HTML `.modal h2` gradient-clip rule.
                Text(
                    text = stringResource(R.string.game_lose_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet)),
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(SpacingTokens.sm))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(SpacingTokens.xl))
                ArrowMazeButton(
                    text = stringResource(R.string.common_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Primary,
                )
                if (canUseHint) {
                    Spacer(Modifier.height(SpacingTokens.sm))
                    ArrowMazeButton(
                        text = "Use Hint",
                        onClick = onUseHint,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Tonal,
                        leadingIcon = ArrowMazeIcons.Hint,
                    )
                }
            }
        }
    }
}

/** Small label-above-value stat block used inside [WinDialog]. */
@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(SolidColor(MaterialTheme.colorScheme.outlineVariant)),
    )
}

/** Formats a duration in seconds as `M:SS` for the [WinDialog] time stat. */
private fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val secs = safeSeconds % 60
    return "%d:%02d".format(minutes, secs)
}

/**
 * Inline helper that applies a graphics-layer alpha to a Modifier — keeps
 * the popIn animation readable without pulling in another import at every
 * call site.
 */
private fun Modifier.graphicsLayerAlpha(alphaValue: Float): Modifier =
    this.alpha(alphaValue)

/** Duration of the HTML `.modal` popIn keyframe — `animation: popIn .32s …`. */
private const val POP_IN_DURATION_MS: Int = 320
