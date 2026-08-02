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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.statistics.LevelBucket
import kotlin.math.max

/**
 * Vertical-bar chart showing how many solved levels fall into each 25-level
 * bucket. Each bar grows from 0 to its target height via
 * [animateFloatAsState] so the chart "rises" into view.
 */
@Composable
fun LevelDistributionChart(
    buckets: List<LevelBucket>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val targetProgress by animateFloatAsState(
        targetValue = if (buckets.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 900),
        label = "level-dist-grow",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Text(
            text = "Levels solved by range",
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        if (buckets.isEmpty() || buckets.all { it.count == 0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No levels solved yet",
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
                val padBottom = 28f
                val padTop = 12f
                val plotH = h - padBottom - padTop

                val maxCount = max(1, buckets.maxOf { it.count })
                val barCount = buckets.size
                val gap = 8f
                val totalGap = gap * (barCount - 1)
                val barWidth = ((w - totalGap) / barCount).coerceAtLeast(2f)

                buckets.forEachIndexed { idx, bucket ->
                    val targetHeight = (bucket.count.toFloat() / maxCount) * plotH
                    val animHeight = targetHeight * targetProgress
                    val left = idx * (barWidth + gap)
                    val top = padTop + (plotH - animHeight)
                    val color = if (bucket.count > 0) cs.primary else cs.outlineVariant
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, animHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }

                // X-axis baseline.
                drawLine(
                    color = cs.outlineVariant,
                    start = Offset(0f, h - padBottom),
                    end = Offset(w, h - padBottom),
                    strokeWidth = 1f,
                )
            }
        }
        // Bucket labels under the bars.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.rangeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Suppress("unused") // Reserved for future overlay tooltips.
private fun Color.withAlpha(alpha: Float): Color = copy(alpha = alpha)
