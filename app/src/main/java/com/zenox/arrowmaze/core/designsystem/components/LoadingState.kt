package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Centred indeterminate loading indicator with an optional message.
 * Drops in wherever a ViewModel exposes a `Loading` state.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = cs.primary,
            strokeWidth = 3.dp,
        )
        if (message != null) {
            Spacer(Modifier.height(SpacingTokens.lg))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * Shimmering placeholder rectangle. Paints a `surfaceVariant` base with
 * a moving lighter highlight band built from [Brush.linearGradient]. Use
 * for list-item placeholders, image placeholders, etc.
 *
 * @param modifier sizing / positioning modifier (caller controls size).
 * @param cornerRadius corner radius of the shimmer shape.
 * @param shape optional custom shape override (defaults to rounded).
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    shape: Shape? = null,
) {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )

    val baseColor = cs.surfaceVariant
    val highlightColor = lerp(start = baseColor, stop = cs.surface, fraction = 0.6f)
    val resolvedShape = shape ?: androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(resolvedShape)
            .drawWithCache {
                val shimmerWidth = size.width
                val start = -shimmerWidth + progress * (2 * shimmerWidth)
                val brush = Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(start, 0f),
                    end = Offset(start + shimmerWidth, 0f),
                )
                onDrawBehind {
                    drawRect(brush = brush)
                }
            },
    )
}
