package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenox.arrowmaze.core.designsystem.tokens.MotionTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Visual variants for [ArrowMazeButton]. Each variant maps to a Material 3
 * colour role so the same component can express hierarchy across screens.
 */
enum class ButtonStyle { Primary, Secondary, Tonal, Glass, Outline }

/**
 * Branded Material 3 button with optional leading/trailing icons and an
 * inline loading state. Touch target ≥ 48.dp on every variant.
 *
 * Visual parity with the HTML reference (Phase AUDIT-1):
 *  - [ButtonStyle.Primary]   — brand-blue→violet gradient fill, 18dp radius,
 *                              17sp bold label, animated `.sheen` overlay
 *                              sweeping left→right every 3 seconds, and a
 *                              `scale(.95)` press feedback matching the
 *                              HTML `.btn:active { transform:scale(.95) }`
 *                              rule with `transition:transform .12s ease`.
 *  - [ButtonStyle.Secondary] — filled, secondary container (brand violet).
 *  - [ButtonStyle.Tonal]     — filled tonal, secondaryContainer.
 *  - [ButtonStyle.Outline]   — outlined with primary-coloured label.
 *  - [ButtonStyle.Glass]     — translucent surface + 15% on-surface border
 *                              for use over gradient backgrounds (auth/home).
 */
@Composable
fun ArrowMazeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isLoading: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    val effectiveEnabled = enabled && !isLoading

    val rowContent: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColorFor(style, cs).let {
                    if (effectiveEnabled) it else cs.onSurfaceVariant
                },
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(SpacingTokens.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                ),
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(SpacingTokens.sm))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    when (style) {
        ButtonStyle.Primary -> {
            // Press-driven scale matching the HTML .btn:active rule.
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (pressed) 0.95f else 1f,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationShort2,
                    easing = MotionTokens.PressEasing,
                ),
                label = "primary-press-scale",
            )

            // Brand gradient container (blue → violet) — matches HTML
            // `linear-gradient(135deg,var(--accent1),var(--accent2))`.
            val brandBrush = Brush.linearGradient(
                colors = listOf(
                    com.zenox.arrowmaze.core.designsystem.theme.BrandBlue,
                    com.zenox.arrowmaze.core.designsystem.theme.BrandViolet,
                ),
            )

            // Sheen overlay: a translucent diagonal gradient sweeping across
            // the button every 3 seconds — matches the HTML `.sheen` element
            // with `animation:sheen 3s ease infinite`.
            val sheenTransition = rememberInfiniteTransition(label = "btn-sheen")
            val sheenProgress by sheenTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = SHEEN_CYCLE_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "btn-sheen-progress",
            )

            Surface(
                modifier = modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .scale(pressScale)
                    .clip(shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                        enabled = effectiveEnabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                shape = shape,
                color = Color.Transparent,
                contentColor = Color.White,
            ) {
                Box {
                    // Gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawRect(brush = brandBrush)
                                drawContent()
                            },
                    )
                    // Sheen overlay
                    if (effectiveEnabled) {
                        SheenOverlay(progress = sheenProgress, modifier = Modifier.fillMaxSize())
                    }
                    Row(
                        modifier = Modifier
                            .padding(contentPadding)
                            .align(Alignment.Center),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = rowContent,
                    )
                }
            }
        }

        ButtonStyle.Secondary -> Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = effectiveEnabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
                disabledContainerColor = cs.surfaceVariant,
                disabledContentColor = cs.onSurfaceVariant,
            ),
            contentPadding = contentPadding,
            content = rowContent,
        )

        ButtonStyle.Tonal -> Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = effectiveEnabled,
            shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = cs.secondaryContainer,
                contentColor = cs.onSecondaryContainer,
                disabledContainerColor = cs.surfaceVariant,
                disabledContentColor = cs.onSurfaceVariant,
            ),
            contentPadding = contentPadding,
            content = rowContent,
        )

        ButtonStyle.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = effectiveEnabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = cs.primary,
                disabledContentColor = cs.onSurfaceVariant,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (effectiveEnabled) cs.outline else cs.outlineVariant,
            ),
            contentPadding = contentPadding,
            content = rowContent,
        )

        ButtonStyle.Glass -> {
            val container = cs.surface.copy(alpha = 0.72f)
            val containerDisabled = cs.surfaceVariant.copy(alpha = 0.4f)
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                modifier = modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        enabled = effectiveEnabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                shape = shape,
                color = if (effectiveEnabled) container else containerDisabled,
                contentColor = if (effectiveEnabled) cs.onSurface else cs.onSurfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = cs.onSurface.copy(alpha = if (effectiveEnabled) 0.18f else 0.08f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = rowContent,
                )
            }
        }
    }
}

/**
 * Sweeping diagonal sheen overlay matching the HTML `.sheen` element.
 * The progress (0..1) drives a translucent diagonal gradient sweeping
 * from left-of-button to right-of-button every [SHEEN_CYCLE_MS] ms.
 *
 * Drawn directly on the Canvas — the gradient's transparent endpoints
 * let the underlying brand-gradient background show through, so the
 * sheen visually "wipes" across the button without replacing it.
 */
@Composable
private fun SheenOverlay(progress: Float, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val canvasWidth = size.width
        // Sweep across -80% to +130% of the button width (matches HTML
        // keyframes `0%,60%{left:-80%}100%{left:130%}`).
        val sweepWidth = canvasWidth * 0.6f
        val start = -sweepWidth + progress * (canvasWidth + sweepWidth * 1.5f)
        val end = start + sweepWidth
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0f),
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0f),
                ),
                start = androidx.compose.ui.geometry.Offset(start, 0f),
                end = androidx.compose.ui.geometry.Offset(end, size.height),
            ),
        )
    }
}

/**
 * Circular icon button with a guaranteed 48.dp touch target. Used for
 * top-bar actions, HUD controls, and dialog dismiss affordances.
 */
@Composable
fun ArrowMazeIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    containerColor: Color = Color.Transparent,
) {
    val cs = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = tint ?: cs.onSurfaceVariant,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = cs.onSurfaceVariant.copy(alpha = 0.38f),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Resolves the on-container content colour for a given [ButtonStyle]. */
@Composable
private fun contentColorFor(style: ButtonStyle, cs: ColorScheme): Color =
    when (style) {
        ButtonStyle.Primary -> Color.White
        ButtonStyle.Secondary -> cs.onSecondary
        ButtonStyle.Tonal -> cs.onSecondaryContainer
        ButtonStyle.Outline -> cs.primary
        ButtonStyle.Glass -> cs.onSurface
    }

/** Sheen sweep cycle in ms — matches HTML `.sheen { animation: sheen 3s … }`. */
private const val SHEEN_CYCLE_MS: Int = 3_000
