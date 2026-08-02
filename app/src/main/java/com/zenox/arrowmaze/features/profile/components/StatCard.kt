package com.zenox.arrowmaze.features.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * Small stat tile rendered inside the Profile stats grid. Shows an icon +
 * numeric value above a centred label.
 *
 * @param icon   Leading icon (tinted with [MaterialTheme.colorScheme.primary]).
 * @param value  The numeric / string value (e.g. "1,240").
 * @param label  Caption shown beneath the value (e.g. "Coins").
 * @param modifier Outer layout modifier.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceVariant.copy(alpha = 0.6f),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}
