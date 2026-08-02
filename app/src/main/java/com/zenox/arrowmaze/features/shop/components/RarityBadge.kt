package com.zenox.arrowmaze.features.shop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Rarity

/**
 * Small coloured badge that signals a [ShopItem]'s rarity.
 *
 *  - [Rarity.COMMON]    — neutral grey.
 *  - [Rarity.RARE]      — cool blue.
 *  - [Rarity.EPIC]      — purple.
 *  - [Rarity.LEGENDARY] — gold gradient.
 *  - [Rarity.MYTHIC]    — red→pink gradient.
 */
@Composable
fun RarityBadge(
    rarity: Rarity,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val (background, content) = when (rarity) {
        Rarity.COMMON    -> cs.surfaceVariant to cs.onSurfaceVariant
        Rarity.RARE      -> Color(0xFF2196F3) to Color.White
        Rarity.EPIC      -> Color(0xFF9C27B0) to Color.White
        Rarity.LEGENDARY -> Color(0xFFFFC107) to Color(0xFF3E2700)
        Rarity.MYTHIC    -> Color(0xFFE91E63) to Color.White
    }
    val displayLabel = rarity.name.lowercase().replaceFirstChar { it.uppercase() }

    val brush: Brush = when (rarity) {
        Rarity.LEGENDARY -> Brush.linearGradient(
            colors = listOf(Color(0xFFFFD54F), Color(0xFFFFA000)),
        )
        Rarity.MYTHIC -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF1744), Color(0xFFF50057)),
        )
        else -> Brush.linearGradient(colors = listOf(background, background))
    }

    Box(
        modifier = modifier
            .background(brush = brush, shape = RoundedCornerShape(50))
            .padding(horizontal = SpacingTokens.sm, vertical = 2.dp),
    ) {
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = content,
        )
    }
}
