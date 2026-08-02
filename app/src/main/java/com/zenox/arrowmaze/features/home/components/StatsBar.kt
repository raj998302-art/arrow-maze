package com.zenox.arrowmaze.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.designsystem.components.CoinCounter
import com.zenox.arrowmaze.core.designsystem.components.CoinCounterSize
import com.zenox.arrowmaze.core.designsystem.components.HudPill
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import kotlinx.coroutines.delay

/**
 * Top stats row on the Home screen. Shows three pills side-by-side:
 *
 *  - Coins (CoinCounter — animated roll-up + coin icon).
 *  - Hints (HudPill with the Hint icon).
 *  - Lives (HudPill with the Life icon). When lives < MAX_LIVES, the pill
 *    shows a small countdown ("mm:ss") until the next regen.
 *
 * @param coins              Current coin balance.
 * @param hints              Current hint count.
 * @param lives              Current life count.
 * @param maxLives           Max lives (used to decide whether to show the timer).
 * @param nextLifeRegenMs    Ms until the next life regenerates (0 when at max).
 */
@Composable
fun StatsBar(
    coins: Int,
    hints: Int,
    lives: Int,
    maxLives: Int,
    nextLifeRegenMs: Long,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme

    // Tick the countdown once per second so the displayed mm:ss stays fresh.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nextLifeRegenMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val remainingMs = (nextLifeRegenMs - (System.currentTimeMillis() - nowMs).coerceAtLeast(0L))
        .coerceAtLeast(0L)
    val showTimer = lives < maxLives && nextLifeRegenMs > 0L

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cs.surface.copy(alpha = 0.92f),
        tonalElevation = ElevationTokens.Level1,
        shadowElevation = ElevationTokens.Level2,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Coins — use CoinCounter for the roll-up animation.
            CoinCounter(
                count = coins,
                size = CoinCounterSize.Medium,
            )

            Spacer(Modifier.width(SpacingTokens.sm))

            // Hints pill.
            HudPill(
                icon = ArrowMazeIcons.Hint,
                count = hints,
                contentDescription = "Hints: $hints",
                tint = cs.tertiary,
            )

            Spacer(Modifier.width(SpacingTokens.sm))

            // Lives pill — count + optional countdown overlay.
            LivesPill(
                lives = lives,
                maxLives = maxLives,
                showTimer = showTimer,
                remainingMs = remainingMs,
            )
        }
    }
}

/**
 * Lives pill — a [HudPill] when at MAX_LIVES, otherwise a custom surface
 * that overlays the mm:ss countdown below the count.
 */
@Composable
private fun LivesPill(
    lives: Int,
    maxLives: Int,
    showTimer: Boolean,
    remainingMs: Long,
) {
    val cs = MaterialTheme.colorScheme
    val mm = (remainingMs / 60_000L).toInt().coerceAtLeast(0)
    val ss = ((remainingMs % 60_000L) / 1000L).toInt().coerceIn(0, 59)
    val timerLabel = "%d:%02d".format(mm, ss)

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = cs.surfaceVariant.copy(alpha = 0.92f),
        contentColor = cs.onSurfaceVariant,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ArrowMazeIcons.Life,
                contentDescription = "Lives: $lives",
                tint = if (lives <= 1) cs.error else cs.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "$lives/$maxLives",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.onSurfaceVariant,
                )
                AnimatedVisibility(
                    visible = showTimer,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = "+1 in $timerLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}
