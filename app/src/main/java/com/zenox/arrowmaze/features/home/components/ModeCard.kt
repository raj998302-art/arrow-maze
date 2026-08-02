package com.zenox.arrowmaze.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Visual variants for [ModeCard].
 *
 *  - [Primary]   — large, brand-gradient background with white text. Used
 *                  for the main "Play" CTA so it dominates the grid.
 *  - [Elevated]  — surface + soft shadow. The default mode-card look for
 *                  all secondary entries (Daily / Practice / Shop / …).
 *  - [Disabled]  — greyed-out surface for actions that aren't currently
 *                  available (e.g. daily challenge after completion).
 */
enum class ModeCardVariant { Primary, Elevated, Disabled }

/**
 * Mode card used in the Home grid. Each card has an icon, a label, an
 * optional badge (e.g. "12/101" unlocked achievements, "Completed"), and
 * optional subtitle text.
 *
 * The card is a clickable surface; the entire card is the tap target.
 */
@Composable
fun ModeCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ModeCardVariant = ModeCardVariant.Elevated,
    subtitle: String? = null,
    badge: String? = null,
    badgeColor: Color? = null,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor: Color
    val contentColor: Color
    val iconTint: Color
    val iconBgColor: Color

    when (variant) {
        ModeCardVariant.Primary -> {
            containerColor = Color.Transparent // gradient overlay paints the bg
            contentColor = Color.White
            iconTint = Color.White
            iconBgColor = Color.White.copy(alpha = 0.18f)
        }
        ModeCardVariant.Elevated -> {
            containerColor = cs.surface
            contentColor = cs.onSurface
            iconTint = cs.primary
            iconBgColor = cs.primaryContainer.copy(alpha = 0.6f)
        }
        ModeCardVariant.Disabled -> {
            containerColor = cs.surfaceVariant.copy(alpha = 0.5f)
            contentColor = cs.onSurfaceVariant.copy(alpha = 0.6f)
            iconTint = cs.onSurfaceVariant.copy(alpha = 0.6f)
            iconBgColor = cs.surfaceVariant
        }
    }

    val shape = RoundedCornerShape(20.dp)
    val effectiveEnabled = enabled && variant != ModeCardVariant.Disabled

    val baseModifier = modifier
        .fillMaxWidth()
        .height(112.dp)
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            enabled = effectiveEnabled,
            role = Role.Button,
            onClick = onClick,
        )

    if (variant == ModeCardVariant.Primary) {
        // Gradient-painted surface for the primary Play card.
        Box(
            modifier = baseModifier.background(
                brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet)),
                shape = shape,
            ),
        ) {
            ModeCardContent(
                label = label,
                subtitle = subtitle,
                icon = icon,
                iconTint = iconTint,
                iconBgColor = iconBgColor,
                contentColor = contentColor,
                badge = badge,
                badgeColor = badgeColor ?: Color.White,
            )
        }
    } else {
        Surface(
            modifier = baseModifier,
            shape = shape,
            color = containerColor,
            tonalElevation = if (variant == ModeCardVariant.Elevated) ElevationTokens.Level1 else ElevationTokens.Level0,
            shadowElevation = if (variant == ModeCardVariant.Elevated) ElevationTokens.Level2 else ElevationTokens.Level0,
        ) {
            ModeCardContent(
                label = label,
                subtitle = subtitle,
                icon = icon,
                iconTint = iconTint,
                iconBgColor = iconBgColor,
                contentColor = contentColor,
                badge = badge,
                badgeColor = badgeColor ?: cs.primary,
            )
        }
    }
}

@Composable
private fun ModeCardContent(
    label: String,
    subtitle: String?,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    contentColor: Color,
    badge: String?,
    badgeColor: Color,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(SpacingTokens.lg)) {
        // Top-right badge (if any)
        if (badge != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(percent = 50),
                color = badgeColor.copy(alpha = 0.18f),
                contentColor = badgeColor,
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = SpacingTokens.sm, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Icon chip
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(SpacingTokens.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}
