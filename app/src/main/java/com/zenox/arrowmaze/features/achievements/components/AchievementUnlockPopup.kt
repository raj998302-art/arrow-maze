package com.zenox.arrowmaze.features.achievements.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.MotionTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Achievement
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated popup shown when a new [Achievement] unlocks.
 *
 * Composition:
 *  - Full-screen scrim (semi-transparent black).
 *  - Centred surface with a radial halo behind the icon, scaling in via
 *    [MotionTokens.OvershootEasing] for the satisfying "pop".
 *  - 18 sparkle particles randomly distributed around the icon, animated
 *    via a [rememberInfiniteTransition] so they twinkle in/out.
 *  - Title + description + reward chips + "Claim" button.
 *
 * The popup is dismissed by tapping the "Claim" button (which calls
 * [onDismiss]) or tapping outside the surface.
 */
@Composable
fun AchievementUnlockPopup(
    achievement: Achievement?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = achievement != null,
        enter = fadeIn(tween(MotionTokens.DurationShort)),
        exit = fadeOut(tween(MotionTokens.DurationShort)),
        modifier = modifier.fillMaxSize(),
    ) {
        if (achievement == null) return@AnimatedVisibility
        val cs = MaterialTheme.colorScheme

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cs.surface,
                tonalElevation = ElevationTokens.Level3,
                shadowElevation = ElevationTokens.Level4,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpacingTokens.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PopupSparkles()
                    Spacer(Modifier.height(SpacingTokens.sm))
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialScale = 0.4f,
                        ) + fadeIn(tween(MotionTokens.DurationMedium)),
                        exit = scaleOut(tween(MotionTokens.DurationShort)),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(cs.tertiary.copy(alpha = 0.5f), Color.Transparent),
                                    ),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = ArrowMazeIcons.Trophy,
                                contentDescription = null,
                                tint = cs.tertiary,
                                modifier = Modifier.size(60.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(SpacingTokens.lg))

                    Text(
                        text = "Achievement Unlocked!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = cs.primary,
                    )
                    Spacer(Modifier.height(SpacingTokens.sm))
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = cs.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(SpacingTokens.xs))
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(SpacingTokens.lg))
                    RewardRow(achievement)
                    Spacer(Modifier.height(SpacingTokens.lg))
                    ArrowMazeButton(
                        text = "Claim",
                        onClick = onDismiss,
                        style = ButtonStyle.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupSparkles() {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "achievement-sparkles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkle-phase",
    )
    val sparkles = remember(0) {
        // 18 deterministic particles around a circle.
        List(18) { i ->
            val angle = (i * 20).toDouble()
            val radius = 80f + (i % 3) * 12f
            Offset(
                x = (cos(Math.toRadians(angle)) * radius).toFloat(),
                y = (sin(Math.toRadians(angle)) * radius).toFloat(),
            ) to (i % 5 == 0)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawBehind {
                    sparkles.forEachIndexed { idx, (offset, isGold) ->
                        val scale = 0.6f + 0.4f * (if (idx % 2 == 0) phase else 1f - phase)
                        val color = (if (isGold) cs.tertiary else cs.primary).copy(
                            alpha = 0.4f + 0.6f * phase,
                        )
                        drawCircle(
                            color = color,
                            radius = 4f * scale,
                            center = Offset(center.x + offset.x, center.y + offset.y),
                        )
                    }
                },
        )
    }
}

@Composable
private fun RewardRow(achievement: Achievement) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        RewardItem(label = "XP", value = "+${achievement.xpReward}", tint = cs.primary)
        RewardItem(label = "Coins", value = "+${achievement.coinReward}", tint = cs.tertiary)
    }
}

@Composable
private fun RewardItem(label: String, value: String, tint: Color) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = tint,
        )
    }
}
