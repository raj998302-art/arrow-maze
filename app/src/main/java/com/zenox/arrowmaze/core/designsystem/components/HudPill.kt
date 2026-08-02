package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Pill-shaped HUD indicator showing an icon + count. Designed for the
 * in-game HUD (coins / hints / lives). Count changes animate via a
 * medium-bouncy spring so the number "pops" when the player gains or
 * spends a resource.
 *
 * @param icon vector icon (e.g. [com.zenox.arrowmaze.core.designsystem.icons.Coin]).
 * @param count current value to display.
 * @param contentDescription accessibility description for the icon.
 * @param tint optional icon tint; defaults to `onSurfaceVariant`.
 */
@Composable
fun HudPill(
    icon: ImageVector,
    count: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val cs = MaterialTheme.colorScheme
    val iconTint = tint ?: cs.onSurfaceVariant

    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "hud-pill-count",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = cs.surfaceVariant.copy(alpha = 0.92f),
        contentColor = cs.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SpacingTokens.md,
                vertical = SpacingTokens.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            Text(
                text = animatedCount.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = cs.onSurfaceVariant,
            )
        }
    }
}
