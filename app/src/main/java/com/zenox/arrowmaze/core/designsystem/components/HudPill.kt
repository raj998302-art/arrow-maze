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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Pill-shaped HUD indicator showing an icon + count. Designed for the
 * in-game HUD (coins / hints / lives). Count changes animate via a
 * medium-bouncy spring so the number "pops" when the player gains or
 * spends a resource.
 *
 * Visual parity with the HTML reference (Phase AUDIT-1):
 *  - Glass background — translucent surface + 14dp rounded corners + soft
 *    brand-tinted shadow, matching `.hud-chip { background:var(--glass);
 *    backdrop-filter:blur(8px); border-radius:14px; box-shadow:var(--shadow) }`.
 *  - Bold 14sp label matching `.hud-chip { font-weight:800;font-size:14px }`.
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

    // Glass surface — 0.72 alpha matches the HTML `--glass:rgba(255,255,255,.72)`
    // token. The brand-tinted shadow replaces the Material default shadow so
    // the pill "lifts" off gradient backgrounds the way it does in the HTML.
    Surface(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = BrandBlue.copy(alpha = 0.14f),
                spotColor = BrandViolet.copy(alpha = 0.16f),
            ),
        shape = RoundedCornerShape(14.dp),
        color = cs.surface.copy(alpha = 0.72f),
        contentColor = cs.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SpacingTokens.sm,
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
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                ),
                color = cs.onSurface,
            )
        }
    }
}
