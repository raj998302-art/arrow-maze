package com.zenox.arrowmaze.features.friends.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zenox.arrowmaze.core.common.flagForCountry
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Friend
import com.zenox.arrowmaze.core.domain.model.FriendStatus

/**
 * Single row in the search-results list. Renders the avatar, name + country
 * flag, level, and an "Add friend" button (or a status pill if there's
 * already a relationship — PENDING_SENT / BLOCKED etc.).
 */
@Composable
fun PlayerSearchResult(
    player: Friend,
    onSendRequest: (Friend) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val initials = remember(player.displayName) {
        player.displayName.split(' ').take(2).joinToString("") { word ->
            word.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        }.ifEmpty { "?" }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cs.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!player.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = "${player.displayName} avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(SpacingTokens.sm))
                    Text(
                        text = flagForCountry(player.country),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "Level ${player.level}  •  ${player.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // Trailing affordance depends on the current relationship state.
            // The Add button only appears when there's no existing relationship
            // (Phase 10 will surface truly unknown players via Firestore search).
            val canAdd = player.status != FriendStatus.ACCEPTED &&
                player.status != FriendStatus.PENDING_SENT &&
                player.status != FriendStatus.PENDING_RECEIVED &&
                player.status != FriendStatus.BLOCKED
            when (player.status) {
                FriendStatus.ACCEPTED -> StatusPill("Friend", cs.secondaryContainer, cs.onSecondaryContainer)
                FriendStatus.PENDING_SENT -> StatusPill("Pending", cs.tertiaryContainer, cs.onTertiaryContainer)
                FriendStatus.PENDING_RECEIVED -> StatusPill("Wants to add you", cs.tertiaryContainer, cs.onTertiaryContainer)
                FriendStatus.BLOCKED -> StatusPill("Blocked", cs.errorContainer, cs.onErrorContainer)
            }
            if (canAdd) {
                ArrowMazeButton(
                    text = "Add",
                    onClick = { onSendRequest(player) },
                    style = ButtonStyle.Primary,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
