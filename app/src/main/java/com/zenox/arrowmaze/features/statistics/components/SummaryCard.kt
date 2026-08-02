package com.zenox.arrowmaze.features.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Compact summary card used at the top of the Statistics screen. Each card
 * surfaces a single headline value + label + supporting icon.
 *
 * Used for: total games, win rate, fastest solve, best streak, average solve
 * time, total time played.
 */
@Composable
fun SummaryCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cs.surface,
        contentColor = cs.onSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        ) {
            Row(
                modifier = Modifier
                    .size(32.dp)
                    .background(cs.primaryContainer, shape = RoundedCornerShape(50)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(SpacingTokens.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
