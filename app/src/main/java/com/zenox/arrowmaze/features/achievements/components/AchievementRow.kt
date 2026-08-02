package com.zenox.arrowmaze.features.achievements.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.AchievementCategory
import com.zenox.arrowmaze.features.achievements.AchievementDisplay

/**
 * Single row in the achievements list. Shows:
 *  - Icon (Trophy when unlocked, Lock when locked; hidden achievements get
 *    a generic Sparkle placeholder).
 *  - Title + description (hidden achievements show "???" instead).
 *  - Reward chips (XP + coins).
 *  - Progress bar (only when not unlocked AND progress target > 0).
 *
 * Unlocked achievements get a subtle golden glow (a `Brush.radialGradient`
 * halo behind the icon + an `ElevationTokens.Level1` shadow).
 */
@Composable
fun AchievementRow(
    display: AchievementDisplay,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val ach = display.achievement
    val isHiddenLocked = ach.isHidden && !display.isUnlocked

    val rowModifier = if (onClick != null) modifier.then(Modifier) else modifier

    Surface(
        modifier = rowModifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        tonalElevation = ElevationTokens.Level1,
        shadowElevation = if (display.isUnlocked) ElevationTokens.Level2 else ElevationTokens.Level0,
    ) {
        Row(
            modifier = Modifier
                .padding(SpacingTokens.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
        ) {
            // Icon (with golden glow when unlocked)
            AchievementIcon(display = display)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHiddenLocked) "Hidden Achievement" else ach.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (display.isUnlocked) cs.onSurface else cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isHiddenLocked) "Keep playing to reveal." else ach.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!display.isUnlocked && display.progressTarget > 0) {
                    Spacer(Modifier.height(SpacingTokens.sm))
                    AchievementProgressBar(
                        progress = display.progress,
                        target = display.progressTarget,
                    )
                }
                Spacer(Modifier.height(SpacingTokens.xs))
                RewardChips(display = display)
            }
        }
    }
}

@Composable
private fun AchievementIcon(display: AchievementDisplay) {
    val cs = MaterialTheme.colorScheme
    val iconTint = if (display.isUnlocked) cs.tertiary else cs.onSurfaceVariant
    val iconVector = when {
        display.isUnlocked -> ArrowMazeIcons.Trophy
        display.achievement.isHidden -> ArrowMazeIcons.Sparkle
        else -> ArrowMazeIcons.Lock
    }
    // HTML reference: locked achievements render their icon with
    // `filter:grayscale(1) opacity(.4)`. Compose's ColorMatrix applies
    // the same desaturation; `alpha(.4f)` matches the opacity rule.
    // Material3 `Icon` has no `colorFilter` parameter, so we render via
    // `foundation.Image` which exposes the full `ColorFilter` API. For
    // unlocked icons we use `ColorFilter.tint(..., SrcIn)` to mirror the
    // `Icon(tint = …)` behaviour; for locked ones we apply the grayscale
    // matrix on top of the original vector colors.
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val colorFilter = if (display.isUnlocked) {
        ColorFilter.tint(iconTint, BlendMode.SrcIn)
    } else {
        ColorFilter.colorMatrix(grayscaleMatrix)
    }
    val iconAlpha = if (display.isUnlocked) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(
                elevation = if (display.isUnlocked) 6.dp else 0.dp,
                shape = CircleShape,
                ambientColor = if (display.isUnlocked) cs.tertiary.copy(alpha = 0.4f) else Color.Transparent,
                spotColor = if (display.isUnlocked) cs.tertiary.copy(alpha = 0.6f) else Color.Transparent,
            )
            .background(
                brush = if (display.isUnlocked) {
                    Brush.radialGradient(
                        colors = listOf(cs.tertiary.copy(alpha = 0.25f), cs.surface),
                    )
                } else {
                    Brush.linearGradient(listOf(cs.surfaceVariant, cs.surfaceVariant))
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = iconVector,
            contentDescription = null,
            colorFilter = colorFilter,
            modifier = Modifier
                .size(24.dp)
                .alpha(iconAlpha),
        )
    }
}

@Composable
private fun RewardChips(display: AchievementDisplay) {
    val cs = MaterialTheme.colorScheme
    val ach = display.achievement
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
        if (ach.xpReward > 0) {
            RewardChip(
                icon = ArrowMazeIcons.Sparkle,
                text = "+${ach.xpReward} XP",
                tint = cs.primary,
            )
        }
        if (ach.coinReward > 0) {
            RewardChip(
                icon = ArrowMazeIcons.Coin,
                text = "+${ach.coinReward}",
                tint = cs.tertiary,
            )
        }
        CategoryLabel(category = ach.category)
    }
}

@Composable
private fun RewardChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = cs.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.sm, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryLabel(category: AchievementCategory) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = category.name.lowercase().replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall,
        color = cs.onSurfaceVariant.copy(alpha = 0.7f),
    )
}
