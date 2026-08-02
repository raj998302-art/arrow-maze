package com.zenox.arrowmaze.core.firebase.messaging

import androidx.annotation.DrawableRes
import com.zenox.arrowmaze.R

/**
 * Maps the FCM data-payload `type` field to a notification channel + icon +
 * default title/body template.
 *
 * The server sends pushes with a `type` field (e.g. `daily_reward`,
 * `come_back`, …); this enum resolves that string into the UI metadata the
 * [ArrowMazeMessagingService] needs to build a [NotificationCompat] payload.
 *
 * Fallback: an unknown `type` string resolves to [GENERAL] so the user
 * still gets *some* notification rather than a silent drop.
 *
 * Server-supplied `title` / `body` fields (when present) override the
 * default templates — the values here are only used when the payload
 * omits them.
 */
enum class NotificationType(
    val wireKey: String,
    val channelId: String,
    @DrawableRes val smallIconRes: Int,
    val defaultTitle: String,
    val defaultBody: String,
) {
    DAILY_REWARD(
        wireKey = "daily_reward",
        channelId = NotificationChannels.CHANNEL_DAILY,
        smallIconRes = R.drawable.ic_splash_icon,
        defaultTitle = "Daily Challenge Awaits!",
        defaultBody = "Solve today's puzzle and keep your streak alive.",
    ),
    COME_BACK(
        wireKey = "come_back",
        channelId = NotificationChannels.CHANNEL_GENERAL,
        smallIconRes = R.drawable.ic_splash_icon,
        defaultTitle = "We Miss You!",
        defaultBody = "Your arrow maze is waiting. Come back and play!",
    ),
    EVENT(
        wireKey = "event",
        channelId = NotificationChannels.CHANNEL_EVENTS,
        smallIconRes = R.drawable.ic_splash_icon,
        defaultTitle = "New Event!",
        defaultBody = "A limited-time event just started — tap to view.",
    ),
    CHALLENGE(
        wireKey = "challenge",
        channelId = NotificationChannels.CHANNEL_EVENTS,
        smallIconRes = R.drawable.ic_splash_icon,
        defaultTitle = "New Challenge!",
        defaultBody = "A new challenge is ready for you.",
    ),
    FRIEND_ACTIVITY(
        wireKey = "friend_activity",
        channelId = NotificationChannels.CHANNEL_SOCIAL,
        smallIconRes = R.drawable.ic_splash_icon,
        defaultTitle = "Friend Activity",
        defaultBody = "Your friends are playing Arrow Maze!",
    ),
    GENERAL(
        wireKey = "general",
        channelId = NotificationChannels.CHANNEL_GENERAL,
        smallIconRes = R.drawable.ic_splash_icon,
        defaultTitle = "Arrow Maze",
        defaultBody = "You have a new update.",
    );

    companion object {
        /** Looks up the [NotificationType] for the given wire key, defaulting to [GENERAL]. */
        fun fromWireKey(key: String?): NotificationType =
            entries.firstOrNull { it.wireKey.equals(key, ignoreCase = true) } ?: GENERAL
    }
}
