package com.zenox.arrowmaze.features.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Daily-streak banner. Shown above the mode-card grid when the player has
 * a non-zero streak. The flame icon gently pulses to draw the eye; the
 * surface uses a warm orange→amber gradient so it reads as a celebration
 * banner even at a glance.
 *
 * @param streak Current streak count (must be > 0 for the banner to be
 *               meaningfully rendered — the parent screens it out at 0).
 */
@Composable
fun DailyStreakBanner(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "streak-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streak-pulse-scale",
    )

    val flameColor = Color(0xFFFF5722)
    val flameAccent = Color(0xFFFFC107)
    val bannerBg = Brush.linearGradient(colors = listOf(flameColor, flameAccent))

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = ElevationTokens.Level1,
        shadowElevation = ElevationTokens.Level2,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = bannerBg, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(pulse),
                )
                Spacer(Modifier.width(SpacingTokens.sm))
                Text(
                    text = "$streak day streak",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            Text(
                text = if (streak >= 7) "🔥 On fire!" else "Keep it up!",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}
