package com.zenox.arrowmaze.features.shop.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.theme.GamePalette
import com.zenox.arrowmaze.core.designsystem.theme.toGamePalette
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.GameTheme
import com.zenox.arrowmaze.core.domain.model.ShopCategory
import com.zenox.arrowmaze.core.domain.model.ShopItem

/**
 * Renders a preview of a [ShopItem] based on its category.
 *
 *  - THEME             — gradient swatch using the theme's [GamePalette].
 *  - ARROW_SKIN        — a single arrow icon with a skin-derived tint.
 *  - TRAIL_FX          — animated sparkle particles in the trail colours.
 *  - BOARD_BACKGROUND  — a small grid pattern using the theme's cellEmpty / boardFrame.
 *  - HINT_PACK         — hint icon + quantity number.
 *  - COIN_PACK         — coin icon + quantity number.
 *  - SEASONAL          — snowflake-like decorative pattern (uses Sparkle).
 *  - PREMIUM_BUNDLE    — trophy with golden halo.
 *  - LIMITED_EDITION   — sparkle + lock overlay.
 */
@Composable
fun ItemPreview(
    item: ShopItem,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        when (item.category) {
            ShopCategory.THEME -> ThemeSwatch(item)
            ShopCategory.ARROW_SKIN -> ArrowSkinPreview(item)
            ShopCategory.TRAIL_FX -> TrailFxPreview(item)
            ShopCategory.BOARD_BACKGROUND -> BoardBgPreview(item)
            ShopCategory.HINT_PACK -> QuantityPreview(item, ArrowMazeIcons.Hint, cs.tertiary)
            ShopCategory.COIN_PACK -> QuantityPreview(item, ArrowMazeIcons.Coin, cs.tertiary)
            ShopCategory.SEASONAL -> SeasonalPreview(item)
            ShopCategory.PREMIUM_BUNDLE -> PremiumBundlePreview(item)
            ShopCategory.LIMITED_EDITION -> LimitedEditionPreview(item)
        }
    }
}

@Composable
private fun ThemeSwatch(item: ShopItem) {
    val themeId = item.id.removePrefix("theme_")
    val theme = GameTheme.ALL_THEMES.firstOrNull { it.id == themeId }
        ?: GameTheme.byId("light")
    val palette: GamePalette = theme.toGamePalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(palette.trailStart, palette.trailEnd),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(SpacingTokens.md)
                .size(28.dp)
                .background(color = palette.arrowFill, shape = RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun ArrowSkinPreview(item: ShopItem) {
    val tint = when (item.id) {
        "arrow_classic"  -> MaterialTheme.colorScheme.primary
        "arrow_neon"     -> Color(0xFF39FF14)
        "arrow_gold"     -> Color(0xFFFFD700)
        "arrow_rainbow"  -> Color(0xFFFF6EC7)
        "arrow_galaxy"   -> Color(0xFFE040FB)
        "arrow_mythic"   -> Color(0xFFFF1744)
        else             -> MaterialTheme.colorScheme.primary
    }
    Icon(
        imageVector = ArrowMazeIcons.ArrowUp,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(40.dp),
    )
}

@Composable
private fun TrailFxPreview(item: ShopItem) {
    val colors: List<Color> = when (item.id) {
        "trail_none"      -> listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E))
        "trail_sparkle"   -> listOf(Color(0xFFFFD700), Color(0xFFFFEB3B))
        "trail_fire"      -> listOf(Color(0xFFFF5722), Color(0xFFFFC107))
        "trail_lightning" -> listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
        "trail_rainbow"   -> listOf(
            Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00),
            Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9),
        )
        "trail_cosmic"    -> listOf(Color(0xFF7C4DFF), Color(0xFFE040FB), Color(0xFF000000))
        else              -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    }
    val transition = rememberInfiniteTransition(label = "trail-fx")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "trail-phase",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                val segments = 24
                val stepX = w / segments
                val stepY = h / 2
                repeat(segments) { i ->
                    val x = i * stepX
                    val y = stepY + (if (i % 2 == 0) -1 else 1) * 6f * (phase * 4 - (i.toFloat() / segments))
                    val colorIndex = (i + (phase * colors.size).toInt()) % colors.size
                    drawCircle(
                        color = colors[colorIndex].copy(alpha = 0.9f),
                        radius = 4f,
                        center = Offset(x, y.coerceIn(8f, h - 8f)),
                    )
                }
            },
    )
}

@Composable
private fun BoardBgPreview(item: ShopItem) {
    val cs = MaterialTheme.colorScheme
    val baseColor = when (item.id) {
        "bg_default"    -> cs.surface
        "bg_parchment"  -> Color(0xFFF4E4BC)
        "bg_chalkboard" -> Color(0xFF2B2B2B)
        "bg_blueprint"  -> Color(0xFF1B5FAA)
        "bg_hexgrid"    -> Color(0xFFEDE7F6)
        "bg_starfield"  -> Color(0xFF0A0E27)
        else            -> cs.surface
    }
    val gridColor = if (item.id == "bg_chalkboard" || item.id == "bg_blueprint" || item.id == "bg_starfield") {
        Color.White.copy(alpha = 0.4f)
    } else {
        Color.Black.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = baseColor)
            .drawBehind {
                val step = size.minDimension / 4f
                var x = step
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                    y += step
                }
            },
    )
}

@Composable
private fun QuantityPreview(item: ShopItem, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    val cs = MaterialTheme.colorScheme
    val qty = when {
        item.id.startsWith("hint_pack_") -> item.id.removePrefix("hint_pack_").toIntOrNull() ?: 0
        item.id.startsWith("coin_pack_") -> item.id.removePrefix("coin_pack_").toIntOrNull()?.let { it / 1000 } ?: 0
        else -> 0
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(SpacingTokens.sm))
        Text(
            text = if (qty > 0) "$qty" else "",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = cs.onSurface,
        )
    }
}

@Composable
private fun SeasonalPreview(item: ShopItem) {
    val tint = if (item.id.contains("winter")) Color(0xFFB3E5FC) else Color(0xFFFFAB91)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = ArrowMazeIcons.Sparkle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(2.dp))
        Icon(
            imageVector = ArrowMazeIcons.Sparkle,
            contentDescription = null,
            tint = tint.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp).rotate(45f),
        )
    }
}

@Composable
private fun PremiumBundlePreview(item: ShopItem) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.6f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ArrowMazeIcons.Trophy,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun LimitedEditionPreview(item: ShopItem) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ArrowMazeIcons.Sparkle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

/**
 * Tiny standalone stroke helper so the [TrailFxPreview] Canvas can draw a
 * hollow ring (used during the trail particle phase). Kept private to this
 * file; not part of the public component surface.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.ring(
    color: Color,
    radius: Float,
    center: Offset,
) {
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 2f))
}
