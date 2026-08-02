package com.zenox.arrowmaze.features.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.statistics.WinLossData
import kotlin.math.min

/**
 * Donut chart visualising the player's win/loss split. The winning arc is
 * drawn in [primary] and the losing arc in [error].
 *
 * The chart animates from 0 to its target fraction on first composition via
 * [animateFloatAsState] so it sweeps into view.
 */
@Composable
fun WinLossChart(
    data: WinLossData,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val targetFraction by animateFloatAsState(
        targetValue = data.winFraction,
        animationSpec = tween(durationMillis = 900),
        label = "win-loss-arc",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SpacingTokens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Text(
            text = "Win / Loss",
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(SpacingTokens.md),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val canvasSize = min(size.width, size.height)
                val stroke = canvasSize * 0.18f
                val diameter = canvasSize - stroke
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)

                if (data.total == 0) {
                    // Empty donut — single ring of outlineVariant.
                    drawArc(
                        color = cs.outlineVariant,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                } else {
                    // Background ring (subtle, behind the win arc).
                    drawArc(
                        color = cs.error.copy(alpha = 0.85f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    // Win arc.
                    val sweep = targetFraction * 360f
                    drawArc(
                        color = cs.primary,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                }
            }

            // Centre percentage label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(data.winFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "win rate",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LegendItem(color = cs.primary, label = "Wins", value = data.wins.toString())
            LegendItem(color = cs.error, label = "Losses", value = data.losses.toString())
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Suppress("unused") // Reserved for future tooltips.
private fun Rect.toPath(): Path = Path().apply {
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}
