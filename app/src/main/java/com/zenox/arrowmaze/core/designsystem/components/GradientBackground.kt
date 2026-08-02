package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlueDark
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.theme.BrandVioletDark

/**
 * Full-screen animated diagonal gradient backdrop. Slowly cycles hue by
 * lerping each colour stop toward its neighbour and back over 12 seconds.
 *
 * Defaults to the brand blue→violet→blue-dark palette; pass [colors] to
 * override (e.g. for cosmetic theme variants in Phase 8). Used on auth
 * and home screens where a soft animated brand surface is desirable.
 *
 * @param modifier outer modifier (typically `Modifier.fillMaxSize()`).
 * @param colours palette to cycle through. Must contain ≥ 2 colours.
 * @param content optional content drawn on top of the gradient.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    content: @Composable () -> Unit = {},
) {
    val baseColors: List<Color> = colors ?: listOf(BrandBlue, BrandViolet, BrandBlueDark, BrandVioletDark)
    require(baseColors.size >= 2) { "GradientBackground requires at least 2 colours, got ${baseColors.size}" }

    val transition = rememberInfiniteTransition(label = "gradient-bg")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradient-shift",
    )

    // For each stop, lerp toward the next colour in the list (wrapping).
    // progress 0 → 1 → 0 produces a slow hue oscillation.
    val animatedColors: List<Color> = baseColors.mapIndexed { index, current ->
        val next = baseColors[(index + 1) % baseColors.size]
        lerp(start = current, stop = next, fraction = progress)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(colors = animatedColors)),
    ) {
        content()
    }
}
