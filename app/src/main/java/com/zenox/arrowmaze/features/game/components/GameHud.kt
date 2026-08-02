package com.zenox.arrowmaze.features.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.GameTopBar
import com.zenox.arrowmaze.core.designsystem.components.HudPill
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Heads-up display for the Game screen.
 *
 * Layout
 * ------
 *  - Top: branded [GameTopBar] with the level title, back + settings buttons.
 *  - Middle: a horizontal scroll-friendly row of [HudPill]s for coins, hints,
 *    lives, timer (M:SS), and moves played.
 *  - Bottom: a [Row] containing the "Use Hint" button (Tonal, leading
 *    [ArrowMazeIcons.Hint]) and a circular restart icon button. The hint
 *    button is disabled when the player can't afford a hint
 *    (`hints == 0 && coins < [AppConstants.HINT_COST_COINS]`).
 *
 * The HUD is purely presentational — every interaction is delegated through
 * the supplied callbacks.
 *
 * @param level      The level number for the title ("Level N").
 * @param moves      Moves played so far (drives the moves pill).
 * @param timeMs     Elapsed session time in ms (formatted as M:SS).
 * @param coins      Live player coin balance.
 * @param hints      Live player hint balance.
 * @param lives      Live player life count.
 * @param onHint     Hint button tap.
 * @param onRestart  Restart icon button tap.
 * @param onBack     Back button tap (from [GameTopBar]).
 * @param onSettings Settings icon tap (from [GameTopBar]).
 */
@Composable
fun GameHud(
    level: Int,
    moves: Int,
    timeMs: Long,
    coins: Int,
    hints: Int,
    lives: Int,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canAffordHint = hints > 0 || coins >= AppConstants.HINT_COST_COINS

    Column(modifier = modifier.fillMaxWidth()) {
        GameTopBar(
            title = "Level $level",
            onBack = onBack,
            onSettings = onSettings,
        )

        Spacer(Modifier.height(SpacingTokens.sm))

        // HUD pills row — wraps gracefully on narrow devices.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HudPill(
                icon = ArrowMazeIcons.Coin,
                count = coins,
                contentDescription = "Coins: $coins",
            )
            HudPill(
                icon = ArrowMazeIcons.Hint,
                count = hints,
                contentDescription = "Hints: $hints",
            )
            HudPill(
                icon = ArrowMazeIcons.Life,
                count = lives,
                contentDescription = "Lives: $lives",
            )
            TimerPill(timeMs = timeMs)
            MovesPill(moves = moves)
        }

        Spacer(Modifier.height(SpacingTokens.md))

        // Bottom action row: Hint + Restart.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArrowMazeButton(
                text = "Hint",
                onClick = onHint,
                style = ButtonStyle.Tonal,
                leadingIcon = ArrowMazeIcons.Hint,
                enabled = canAffordHint,
                modifier = Modifier.weight(1f),
            )
            ArrowMazeIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Restart level",
                onClick = onRestart,
            )
        }

        Spacer(Modifier.height(SpacingTokens.sm))
    }
}

/**
 * Pill-shaped timer that displays the elapsed session time as M:SS.
 * Mirrors [HudPill]'s visual language but accepts a `Long` ms value
 * instead of an `Int` count.
 */
@Composable
private fun TimerPill(
    timeMs: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector = ArrowMazeIcons.Target,
) {
    val cs = MaterialTheme.colorScheme
    val seconds = (timeMs / 1000L).toInt().coerceAtLeast(0)
    val minutes = seconds / 60
    val secs = seconds % 60
    val formatted = "%d:%02d".format(minutes, secs)

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
                contentDescription = "Elapsed time",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * Pill-shaped moves-played counter. Uses the [ArrowMazeIcons.Sparkle] icon
 * to distinguish it from the resource pills.
 */
@Composable
private fun MovesPill(
    moves: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
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
                imageVector = ArrowMazeIcons.Sparkle,
                contentDescription = "Moves played",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            Text(
                text = moves.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * Reserved for future skinning hooks — currently unused but kept so the
 * symbol is exported for downstream feature modules.
 */
@Suppress("unused")
private fun defaultHudTint(): Color = Color.Unspecified
