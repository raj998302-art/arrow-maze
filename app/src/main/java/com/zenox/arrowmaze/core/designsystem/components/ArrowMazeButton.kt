package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
 * - [ButtonStyle.Primary]   — filled, primary container.
 * - [ButtonStyle.Secondary] — filled, secondary container (brand violet).
 * - [ButtonStyle.Tonal]     — filled tonal, secondaryContainer.
 * - [ButtonStyle.Outline]   — outlined with primary-coloured label.
 * - [ButtonStyle.Glass]     — translucent surface + 15% on-surface border
 *                             for use over gradient backgrounds (auth/home).
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
    val shape = RoundedCornerShape(16.dp)
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
                style = MaterialTheme.typography.labelLarge,
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
        ButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = effectiveEnabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                disabledContainerColor = cs.surfaceVariant,
                disabledContentColor = cs.onSurfaceVariant,
            ),
            contentPadding = contentPadding,
            content = rowContent,
        )

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
            val container = cs.surface.copy(alpha = 0.55f)
            val containerDisabled = cs.surfaceVariant.copy(alpha = 0.4f)
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                modifier = modifier
                    .defaultMinSize(minHeight = 48.dp)
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
        ButtonStyle.Primary -> cs.onPrimary
        ButtonStyle.Secondary -> cs.onSecondary
        ButtonStyle.Tonal -> cs.onSecondaryContainer
        ButtonStyle.Outline -> cs.primary
        ButtonStyle.Glass -> cs.onSurface
    }
