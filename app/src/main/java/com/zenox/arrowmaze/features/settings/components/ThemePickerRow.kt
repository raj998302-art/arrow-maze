package com.zenox.arrowmaze.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.GameTheme

/**
 * Horizontal scroll of the 13 cosmetic themes from [GameTheme.ALL_THEMES].
 * Each swatch shows the theme's primary + secondary + background colors as
 * a stacked preview; the currently-selected swatch is highlighted with a
 * `primary` border.
 *
 * Tapping a swatch invokes [onThemeSelected]; the parent VM decides whether
 * the user is allowed to switch (e.g. premium gating is enforced upstream).
 */
@Composable
fun ThemePickerRow(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    ownedThemeIds: Set<String> = emptySet(),
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.sm),
        contentPadding = PaddingValues(horizontal = SpacingTokens.sm),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        items(items = GameTheme.ALL_THEMES, key = { it.id }) { theme ->
            ThemeSwatch(
                theme = theme,
                isSelected = theme.id == selectedThemeId,
                isOwned = ownedThemeIds.isEmpty() || theme.id in ownedThemeIds || !theme.isPremium,
                onClick = { onThemeSelected(theme.id) },
            )
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: GameTheme,
    isSelected: Boolean,
    isOwned: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(SpacingTokens.xs)
            .semantics {
                contentDescription = "Theme ${theme.displayName}" +
                    if (isSelected) ", selected" else ""
                role = Role.RadioButton
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Color preview: a circle backdrop with two halves (primary + secondary).
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(parseHex(theme.colors.background)),
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(parseHex(theme.colors.primary)),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(parseHex(theme.colors.secondary)),
                )
            }
        }
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
        if (theme.isPremium && !isOwned) {
            Text(
                text = "Premium",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
            )
        }
    }
    Spacer(Modifier.height(0.dp))
}

/** Parses a `#RRGGBB` (or `#AARRGGBB`) hex string into a Compose [Color]. */
private fun parseHex(hex: String): Color {
    val normalized = hex.removePrefix("#")
    return when (normalized.length) {
        6, 8 -> Color(android.graphics.Color.parseColor("#$normalized"))
        else -> Color.Gray
    }
}
