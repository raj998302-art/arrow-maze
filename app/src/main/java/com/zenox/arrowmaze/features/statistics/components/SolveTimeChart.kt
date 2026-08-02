package com.zenox.arrowmaze.features.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import com.zenox.arrowmaze.features.statistics.SolveTimePoint
import kotlin.math.max

/**
 * Line chart showing the player's solve time (in seconds) per level. The
 * vertical axis auto-scales to the maximum value in the dataset; the
 * horizontal axis is linear in level number.
 *
 * The chart animates from 0 → 1 via [animateFloatAsState] so the line draws
 * itself in.
 */
@Composable
fun SolveTimeChart(
    points: List<SolveTimePoint>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val drawProgress by animateFloatAsState(
        targetValue = if (points.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 900),
        label = "solve-time-draw",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Text(
            text = "Solve time by level",
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No solve times recorded yet",
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
                val padTop = 16f
                val padBottom = 24f
                val plotW = w - padLeft - padRight
                val plotH = h - padTop - padBottom

                val maxMs = points.maxOf { it.solveTimeMs }.coerceAtLeast(1L)
                val minLevel = points.first().level
                val maxLevel = points.last().level
                val levelSpan = max(1, maxLevel - minLevel)

                // Background grid lines (3 horizontal).
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

                // Build the polyline path.
                val linePath = Path()
                points.forEachIndexed { idx, point ->
                    val xRatio = if (points.size == 1) 0.5f else
                        (point.level - minLevel).toFloat() / levelSpan
                    val yRatio = 1f - (point.solveTimeMs.toFloat() / maxMs)
                    val x = padLeft + xRatio * plotW
                    val y = padTop + yRatio * plotH
                    if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }

                // Compute the visible portion of the path via a clip rect
                // based on the draw-progress animation. We approximate by
                // drawing the path under a horizontal sweep that grows from
                // left to right.
                val sweepX = padLeft + drawProgress * plotW
                drawLine(
                    color = cs.primary,
                    start = Offset(padLeft, padTop + plotH / 2f),
                    end = Offset(sweepX, padTop + plotH / 2f),
                    strokeWidth = 0f,
                )
                // Draw the full path with a transparent "fill" gradient below.
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(padLeft + plotW * drawProgress, padTop + plotH)
                    lineTo(padLeft, padTop + plotH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(cs.primary.copy(alpha = 0.35f), Color.Transparent),
                        startY = padTop,
                        endY = padTop + plotH,
                    ),
                )
                // Stroke the line up to the sweep position (approximated by
                // drawing the whole path with alpha — Compose doesn't expose
                // partial path drawing without a custom Shape).
                drawPath(
                    path = linePath,
                    color = cs.primary,
                    style = Stroke(width = 3f),
                )

                // Markers at every point.
                points.forEach { point ->
                    val xRatio = if (points.size == 1) 0.5f else
                        (point.level - minLevel).toFloat() / levelSpan
                    val yRatio = 1f - (point.solveTimeMs.toFloat() / maxMs)
                    val cx = padLeft + xRatio * plotW
                    val cy = padTop + yRatio * plotH
                    drawCircle(
                        color = cs.primary,
                        radius = 4f,
                        center = Offset(cx, cy),
                    )
                }
            }
        }
        // Axis caption row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Level ${points.first().level}",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = "Level ${points.last().level}",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
        Text(
            text = "Time in seconds",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
