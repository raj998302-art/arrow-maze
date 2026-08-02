package com.zenox.arrowmaze.features.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Profile
import kotlin.math.roundToInt

/**
 * Animated XP progress bar. Reads `profile.xpProgress()` and renders a
 * level chip + a horizontal progress track that animates whenever the
 * underlying fraction changes.
 *
 * @param profile   The user's profile (level + xp source of truth).
 * @param modifier  Outer layout modifier.
 */
@Composable
fun LevelProgressBar(
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val (intoLevel, remaining) = remember(profile.xp, profile.level) {
        profile.xpProgress()
    }
    val xpPerLevel = Profile.DEFAULT_XP_PER_LEVEL
    val targetFraction = (intoLevel.toFloat() / xpPerLevel.toFloat()).coerceIn(0f, 1f)

    // Animate fraction changes for a smooth bar fill.
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 600),
        label = "xp-progress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lvl ${profile.level}",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(0.dp))
            }
            Text(
                text = "$intoLevel / $xpPerLevel XP",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
        }

        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(cs.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(cs.primary),
            )
        }

        Text(
            text = "${remaining.coerceAtLeast(0)} XP to next level",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
}

/**
 * Formats a 0..1 fraction into a whole-number percentage string. Used by
 * the Profile stats grid for the win-rate tile.
 */
internal fun Float.toPercentString(): String = "${(this * 100f).roundToInt()}%"

/**
 * Formats a duration in milliseconds as `M:SS` (or `H:MM:SS` when ≥ 1h).
 * Used by the Profile stats grid for the average-solve-time tile.
 */
internal fun Long.formatDuration(): String {
    if (this <= 0L) return "0:00"
    val totalSeconds = this / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
