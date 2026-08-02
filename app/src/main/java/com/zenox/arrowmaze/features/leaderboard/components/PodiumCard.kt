package com.zenox.arrowmaze.features.leaderboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zenox.arrowmaze.core.common.flagForCountry
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry

/**
 * Visual variants for the three podium positions.
 *
 * - [Gold]   — rank 1 (tallest, gold gradient).
 * - [Silver] — rank 2 (medium height, silver gradient).
 * - [Bronze] — rank 3 (shortest, bronze gradient).
 */
enum class PodiumVariant(val height: Int, val gradient: List<Color>) {
    Gold(160, listOf(Color(0xFFFFD54F), Color(0xFFFFB300))),
    Silver(130, listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E))),
    Bronze(100, listOf(Color(0xFFD7CCC8), Color(0xFF8D6E63))),
}

/**
 * Single pedestal card for a top-3 finisher. Renders the medal emoji, the
 * avatar (or initials), the player's name with country flag, and the level
 * + XP footer.
 *
 * The three cards are arranged in the screen as `[Silver, Gold, Bronze]` so
 * the gold card sits visually in the middle.
 */
@Composable
fun PodiumCard(
    entry: LeaderboardEntry,
    variant: PodiumVariant,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val initials = remember(entry.displayName) {
        entry.displayName.split(' ').take(2).joinToString("") { word ->
            word.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        }.ifEmpty { "?" }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        // Medal emoji above the avatar.
        Text(
            text = entry.medal.ifEmpty { "🏆" },
            style = MaterialTheme.typography.headlineMedium,
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (!entry.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = "${entry.displayName} avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = entry.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${flagForCountry(entry.country)} Lvl ${entry.level}",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
        )

        Spacer(Modifier.height(SpacingTokens.xs))

        // Pedestal — gradient box with rank + XP centred.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(variant.height.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Brush.verticalGradient(variant.gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${entry.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
