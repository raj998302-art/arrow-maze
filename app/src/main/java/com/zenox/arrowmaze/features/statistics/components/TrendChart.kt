package com.zenox.arrowmaze.features.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.statistics.TrendPoint
import kotlin.math.max

/**
 * Area + line chart of the last 30 solved levels' solve times. Used as a
 * proxy for "recent games trend" — Phase 10 will replace this with a real
 * per-game history once the Firestore `games` collection ships.
 *
 * The line is drawn with a vertical gradient fill underneath so it reads as
 * a trend area.
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val drawProgress by animateFloatAsState(
        targetValue = if (points.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 900),
        label = "trend-draw",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Text(
            text = "Recent trend (last ${minOf(30, points.size)} levels)",
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        if (points.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Solve at least 2 levels to see a trend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            return@Column
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padLeft = 8f
                val padRight = 8f
                val padTop = 12f
                val padBottom = 24f
                val plotW = w - padLeft - padRight
                val plotH = h - padTop - padBottom

                val maxMs = points.maxOf { it.valueMs }.coerceAtLeast(1L)
                val last = points.size - 1

                // Background grid lines.
                val gridColor = cs.outlineVariant.copy(alpha = 0.3f)
                for (i in 0..3) {
                    val y = padTop + plotH * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padLeft, y),
                        end = Offset(w - padRight, y),
                        strokeWidth = 1f,
                    )
                }

                // Build the line + area paths.
                val linePath = Path()
                points.forEachIndexed { idx, point ->
                    val x = padLeft + (idx.toFloat() / last) * plotW
                    val y = padTop + (1f - point.valueMs.toFloat() / maxMs) * plotH
                    if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    val lastX = padLeft + (last.toFloat() / last) * plotW
                    lineTo(lastX, padTop + plotH)
                    lineTo(padLeft, padTop + plotH)
                    close()
                }

                // Apply draw-progress as an alpha sweep on the area fill.
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            cs.tertiary.copy(alpha = 0.45f * drawProgress),
                            Color.Transparent,
                        ),
                        startY = padTop,
                        endY = padTop + plotH,
                    ),
                )
                drawPath(
                    path = linePath,
                    color = cs.tertiary,
                    style = Stroke(width = 3f),
                    alpha = drawProgress,
                )

                // First + last markers
                val firstX = padLeft
                val firstY = padTop + (1f - points.first().valueMs.toFloat() / maxMs) * plotH
                drawCircle(
                    color = cs.tertiary,
                    radius = 5f,
                    center = Offset(firstX, firstY),
                )
                val lastX = padLeft + (last.toFloat() / last) * plotW
                val lastY = padTop + (1f - points.last().valueMs.toFloat() / maxMs) * plotH
                drawCircle(
                    color = cs.tertiary,
                    radius = 5f,
                    center = Offset(lastX, lastY),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = points.last().label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}
