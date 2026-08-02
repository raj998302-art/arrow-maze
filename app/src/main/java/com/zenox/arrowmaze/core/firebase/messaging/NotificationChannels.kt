package com.zenox.arrowmaze.core.firebase.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Notification channel ids used by [ArrowMazeMessagingService]. Each
 * channel maps to a category of push so the user can independently mute
 * "social" while still receiving "daily rewards", etc.
 */
object NotificationChannels {

    /** Daily reward reminders ("Your daily challenge is waiting!"). */
    const val CHANNEL_DAILY = "daily_rewards"

    /** Friend activity: friend requests, leader overtakes, etc. */
    const val CHANNEL_SOCIAL = "social"

    /** Limited-time events, seasonal challenges. */
    const val CHANNEL_EVENTS = "events"

    /** Catch-all channel for anything that doesn't fit elsewhere. */
    const val CHANNEL_GENERAL = "general"

    /**
     * Creates all four channels on API 26+ (our minSdk). On lower APIs
     * this is a no-op (notifications just use the legacy priority field).
     *
     * Safe to call repeatedly — `createNotificationChannel` is idempotent.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                "Daily Rewards",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily challenge + reward reminders"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SOCIAL,
                "Social",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Friend activity, leader overtakes, and other social updates"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EVENTS,
                "Events",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Limited-time events and seasonal challenges"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "All other Arrow Maze notifications"
            }
        )
    }
}
