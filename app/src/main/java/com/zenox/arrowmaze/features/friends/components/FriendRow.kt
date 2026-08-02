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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zenox.arrowmaze.core.common.flagForCountry
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Friend
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Single row in the accepted-friends list. Shows the avatar, display name
 * (with country flag), level + XP, an online-status dot, and a "last seen"
 * relative timestamp. Trailing overflow menu offers "Remove" and "Block"
 * affordances.
 */
@Composable
fun FriendRow(
    friend: Friend,
    onRemove: (Friend) -> Unit,
    onBlock: (Friend) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val initials = remember(friend.displayName) {
        friend.displayName.split(' ').take(2).joinToString("") { word ->
            word.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        }.ifEmpty { "?" }
    }
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Friend ${friend.displayName}, level ${friend.level}" },
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
            // Avatar with online dot overlay.
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!friend.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = friend.avatarUrl,
                            contentDescription = "${friend.displayName} avatar",
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
                // Online status dot.
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(cs.surface)
                        .padding(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (friend.isOnline) cs.tertiary else cs.outline),
                    )
                }
            }

            // Name + level + last seen
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(SpacingTokens.sm))
                    Text(
                        text = flagForCountry(friend.country),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "Level ${friend.level}  •  ${friend.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = formatLastSeen(friend.isOnline, friend.lastSeenEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }

            // Overflow menu
            Box {
                ArrowMazeIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "More options for ${friend.displayName}",
                    onClick = { menuOpen = true },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove friend") },
                        leadingIcon = { Icon(Icons.Rounded.PersonRemove, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRemove(friend)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Block", color = cs.error) },
                        leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null, tint = cs.error) },
                        onClick = {
                            menuOpen = false
                            onBlock(friend)
                        },
                    )
                }
            }
        }
    }
}

/** Formats the friend's last-seen epoch ms as a humanised relative string. */
private fun formatLastSeen(isOnline: Boolean, lastSeenEpochMs: Long): String {
    if (isOnline) return "Online now"
    val now = System.currentTimeMillis()
    val diff = now - lastSeenEpochMs
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just left"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            "Last seen ${mins}m ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "Last seen ${hours}h ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "Last seen ${days}d ago"
        }
        else -> {
            val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
            "Last seen ${fmt.format(Date(lastSeenEpochMs))}"
        }
    }
}
