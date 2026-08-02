package com.zenox.arrowmaze.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.CardVariant
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeCard
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.DifficultyTier
import com.zenox.arrowmaze.core.domain.model.Profile

/**
 * Hero card on the Home screen. Shows the player's current level, tier badge,
 * XP progress bar, and a primary "Continue" button that resumes the game at
 * the player's current level.
 *
 * When [currentLevel] is greater than the player's [Profile.highestLevel]
 * (i.e. the next level has never been attempted), the CTA label flips to
 * "Next Level" so the player knows they're about to attempt a fresh level.
 *
 * @param profile           Live player profile.
 * @param currentLevel      Level the player is resuming at.
 * @param tier              Tier that [currentLevel] belongs to.
 * @param onContinue        Fired when the user taps the Continue / Next Level CTA.
 */
@Composable
fun HeroLevelCard(
    profile: Profile,
    currentLevel: Int,
    tier: DifficultyTier,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val xpPerLevel = Profile.DEFAULT_XP_PER_LEVEL
    val (xpInto, xpRemaining) = profile.xpProgress(xpPerLevel)
    val progressFraction = (xpInto.toFloat() / xpPerLevel.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "hero-xp-progress",
    )

    val isNextLevel = currentLevel > profile.highestLevel
    val ctaLabel = if (isNextLevel) "Next Level" else "Continue"

    ArrowMazeCard(
        modifier = modifier,
        variant = CardVariant.Gradient,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level $currentLevel",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = cs.onSurface,
                )
                Spacer(Modifier.height(SpacingTokens.xs))
                TierBadge(tier = tier)
            }
            Spacer(Modifier.width(SpacingTokens.md))
            // Decorative target icon over a brand-gradient circle so the
            // card reads as the "game hub" surface at a glance.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ArrowMazeIcons.Target,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.height(SpacingTokens.lg))

        // XP progress row: "XP" label + into/total + animated bar.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "XP",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = "$xpInto / $xpPerLevel",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(SpacingTokens.xs))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50)),
            color = cs.primary,
            trackColor = cs.surfaceVariant,
        )

        Spacer(Modifier.height(SpacingTokens.lg))

        ArrowMazeButton(
            text = ctaLabel,
            onClick = onContinue,
            style = ButtonStyle.Primary,
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Coloured pill showing the difficulty tier's display name + a matching
 * colour dot. The colour comes from [DifficultyTier.colorHex]; we parse it
 * once per recomposition (cheap — six entries total).
 */
@Composable
private fun TierBadge(tier: DifficultyTier) {
    val cs = MaterialTheme.colorScheme
    val dotColor = runCatching { Color(android.graphics.Color.parseColor(tier.colorHex)) }
        .getOrDefault(cs.primary)

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = cs.surfaceVariant.copy(alpha = 0.7f),
        contentColor = cs.onSurfaceVariant,
        tonalElevation = ElevationTokens.Level1,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            Text(
                text = tier.displayName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurfaceVariant,
            )
        }
    }
}
