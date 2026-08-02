package com.zenox.arrowmaze.features.shop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens

/**
 * Pill-shaped category tab for the Shop screen's horizontal category strip.
 *
 * @param label   Display label (e.g. "Themes", "Arrow Skins").
 * @param selected Whether this tab is the active selection.
 * @param onClick Invoked when the user taps the pill.
 */
@Composable
fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val background = if (selected) cs.primary else cs.surfaceVariant.copy(alpha = 0.6f)
    val contentColor = if (selected) cs.onPrimary else cs.onSurfaceVariant
    val borderColor = if (selected) Color.Transparent else cs.outlineVariant

    Box(
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(50))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}
