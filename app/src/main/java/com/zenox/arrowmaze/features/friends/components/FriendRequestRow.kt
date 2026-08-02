package com.zenox.arrowmaze.features.friends.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.FriendRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single row in the friend-requests list. Handles two layouts:
 *
 * - **Incoming** (you received the request): shows "Accept" + "Decline"
 *   buttons.
 * - **Outgoing** (you sent the request): shows a "Pending" tag + "Cancel"
 *   button.
 *
 * Both layouts show the counter-party's name, the message (if any), and a
 * relative timestamp.
 */
@Composable
fun FriendRequestRow(
    request: FriendRequest,
    isIncoming: Boolean,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cs.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.md),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isIncoming) request.fromName else request.toUid,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isIncoming) "Wants to be your friend" else "Awaiting response",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatTimestamp(request.timestampEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            if (!request.message.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Surface(
                    color = cs.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "\u201C${request.message}\u201D",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(SpacingTokens.xs))

            if (isIncoming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                ) {
                    ArrowMazeButton(
                        text = "Accept",
                        onClick = { onAccept(request) },
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Primary,
                    )
                    ArrowMazeButton(
                        text = "Decline",
                        onClick = { onDecline(request) },
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Outline,
                    )
                }
            } else {
                ArrowMazeButton(
                    text = "Cancel request",
                    onClick = { onDecline(request) },
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Outline,
                )
            }
        }
    }
}

private fun formatTimestamp(epochMs: Long): String {
    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return fmt.format(Date(epochMs))
}
