package com.zenox.arrowmaze.features.leaderboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zenox.arrowmaze.core.common.flagForCountry
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry

/**
 * Single row in the leaderboard list (rank 4 and below; the top 3 use
 * [PodiumCard]). Renders: rank number, avatar (or initials), display name +
 * country flag, level, XP, and a coin balance footer.
 *
 * The current user's row is highlighted with the brand `primaryContainer`
 * backdrop so they can spot themselves quickly in a long list.
 */
@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val initials = remember(entry.displayName) {
        entry.displayName.split(' ').take(2).joinToString("") { word ->
            word.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        }.ifEmpty { "?" }
    }
    val rowDescription = "Rank ${entry.rank}: ${entry.displayName}, level ${entry.level}, ${entry.xp} XP"

    val rowModifier = if (entry.isCurrentUser) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.primaryContainer.copy(alpha = 0.45f))
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm)
            .semantics { contentDescription = rowDescription }
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm)
            .semantics { contentDescription = rowDescription }
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
    ) {
        // Rank number
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.isCurrentUser) cs.primary else cs.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cs.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!entry.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = "${entry.displayName} avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Name + country + level
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (entry.isCurrentUser) cs.onPrimaryContainer else cs.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.isCurrentUser) {
                    Spacer(Modifier.width(SpacingTokens.sm))
                    Surface(
                        color = cs.primary,
                        contentColor = cs.onPrimary,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = "${flagForCountry(entry.country)}  Level ${entry.level}  •  ${entry.xp} XP",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Coins footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = entry.coins.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (entry.isCurrentUser) cs.onPrimaryContainer else cs.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = ArrowMazeIcons.Coin,
                contentDescription = "Coins",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
