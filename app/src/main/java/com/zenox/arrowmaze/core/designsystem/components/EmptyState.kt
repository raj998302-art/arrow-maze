package com.zenox.arrowmaze.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Centred empty-state placeholder. Used when a list / grid / history is
 * empty so the user always sees an explanation instead of a blank screen
 * (no daily challenges yet, no friends, no achievements unlocked, etc.).
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SpacingTokens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = cs.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(SpacingTokens.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(SpacingTokens.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(SpacingTokens.xl))
            ArrowMazeButton(
                text = actionText,
                onClick = onAction,
                style = ButtonStyle.Tonal,
            )
        }
    }
}
