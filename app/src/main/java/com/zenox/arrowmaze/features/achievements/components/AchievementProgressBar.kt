package com.zenox.arrowmaze.features.achievements.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Tiny linear progress bar showing X / Y with optional label.
 *
 *  - When `progress >= target`, the bar fills fully in the success colour.
 *  - When `target == 0` (hidden requirements), shows a flat grey bar.
 *  - The fill colour is [MaterialTheme.colorScheme.tertiary] (gold-ish) by
 *    default so progress visually echoes the coin accent.
 *
 * @param progress current progress value (>= 0).
 * @param target the threshold to unlock (>= 1).
 * @param showLabel when true, renders a `"X / Y"` label below the bar.
 */
@Composable
fun AchievementProgressBar(
    progress: Int,
    target: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val safeTarget = target.coerceAtLeast(1)
    val rawFraction = (progress.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 600),
        label = "achievement-progress",
    )

    val trackColor = cs.surfaceVariant
    val fillColor = if (progress >= safeTarget) cs.tertiary else cs.primary

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(fillColor),
        )
        if (showLabel) {
            Text(
                text = "$progress / $safeTarget",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(top = 12.dp),
            )
        }
    }
}
