package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Size preset for [CoinCounter]. Controls icon + spacing; the text style
 * is derived from [MaterialTheme.typography] in the composable.
 */
enum class CoinCounterSize(val iconSize: Dp, val spacing: Dp) {
    Small(iconSize = 14.dp, spacing = SpacingTokens.xs),
    Medium(iconSize = 18.dp, spacing = SpacingTokens.sm),
    Large(iconSize = 26.dp, spacing = SpacingTokens.md),
}

/**
 * Animated coin counter that "rolls up" to the new value.
 *
 * Implementation: the previous count is snapped to (so the spring has a
 * starting point one step away from the target), then `animateIntAsState`
 * springs the displayed value to [count] with a medium-bouncy spring.
 * The result is the satisfying pop-and-roll the game HUD shows when coins
 * are awarded. A [ArrowMazeIcons.Coin] leads the number.
 *
 * @param count current coin balance to display.
 * @param size  visual size preset.
 */
@Composable
fun CoinCounter(
    count: Int,
    modifier: Modifier = Modifier,
    size: CoinCounterSize = CoinCounterSize.Medium,
) {
    val cs = MaterialTheme.colorScheme

    // animateIntAsState snaps the initial value (no animation on first
    // composition), then springs toward the new target on every change.
    // The medium-bouncy spring gives the rolling "pop" feel.
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "coin-counter",
    )

    val textStyle = when (size) {
        CoinCounterSize.Small -> MaterialTheme.typography.labelLarge
        CoinCounterSize.Medium -> MaterialTheme.typography.titleMedium
        CoinCounterSize.Large -> MaterialTheme.typography.titleLarge
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ArrowMazeIcons.Coin,
            contentDescription = null,
            tint = cs.tertiary,
            modifier = Modifier.size(size.iconSize),
        )
        Spacer(Modifier.width(size.spacing))
        Text(
            text = animatedCount.toString(),
            style = textStyle.copy(fontWeight = FontWeight.Bold),
            color = cs.onSurface,
        )
    }
}
