package com.zenox.arrowmaze.core.firebase.messaging

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zenox.arrowmaze.MainActivity
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * FCM messaging service. Receives push notifications and translates the
 * data payload into a [NotificationCompat] notification posted to the
 * per-type channel from [NotificationChannels].
 *
 * Wire format (data payload):
 * ```
 * {
 *   "type": "daily_reward" | "come_back" | "event" | "challenge" |
 *           "friend_activity" | "general",
 *   "title": "Optional override title",
 *   "body":  "Optional override body"
 * }
 * ```
 *
 * The service is `@AndroidEntryPoint`-annotated so Hilt injects the
 * [SettingsRepository] (for the user's `notificationsEnabled` flag) and
 * the [SessionRepository] (for the current uid, used in [onNewToken] to
 * persist the FCM token to Firestore).
 *
 * Notification permission:
 *  - API 33+ requires `POST_NOTIFICATIONS` runtime permission. We check
 *    [NotificationManagerCompat.areNotificationsEnabled] before posting;
 *    if the user has revoked the permission, the push is silently
 *    dropped (the FCM dashboard still records delivery).
 *  - The user-level `notificationsEnabled` setting (in Settings) is also
 *    respected — when `false`, no notifications are posted.
 */
@AndroidEntryPoint
class ArrowMazeMessagingService : FirebaseMessagingService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sessionRepository: SessionRepository

    /** Service-scoped coroutine context — the service has no viewModelScope. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Channels must exist before we post any notification; create them
        // up-front so the first push doesn't silently fail.
        NotificationChannels.createChannels(this)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM received: from=%s data=%s notification=%s",
            message.from, message.data, message.notification)

        scope.launch {
            // Honour the user-level notificationsEnabled setting.
            val settings = settingsRepository.observe().first()
            if (!settings.notificationsEnabled) {
                Timber.d("Skipping notification: notificationsEnabled=false")
                return@launch
            }

            // Honour the system-level permission (API 33+ POST_NOTIFICATIONS).
            if (!NotificationManagerCompat.from(this@ArrowMazeMessagingService)
                    .areNotificationsEnabled()
            ) {
                Timber.d("Skipping notification: system notifications disabled")
                return@launch
            }

            val type = NotificationType.fromWireKey(message.data[KEY_TYPE])
            val title = message.data[KEY_TITLE]
                ?: message.notification?.title
                ?: type.defaultTitle
            val body = message.data[KEY_BODY]
                ?: message.notification?.body
                ?: type.defaultBody

            postNotification(type = type, title = title, body = body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: %s", token)

        scope.launch {
            // 1. Cache the token locally so the rest of the app can read it.
            sessionRepository.setFcmToken(token)

            // 2. Push the token to the user's Firestore document so the
            //    server can target this device for pushes.
            val uid = sessionRepository.currentUidFlow.first()
                ?: FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Timber.d("FCM token not pushed to Firestore — no signed-in user")
                return@launch
            }

            FirebaseFirestore.getInstance()
                .collection(AppConstants.FS_USERS)
                .document(uid)
                .update(FIELD_FCM_TOKEN, token)
                .addOnSuccessListener {
                    Timber.d("FCM token persisted to Firestore for uid=%s", uid)
                }
                .addOnFailureListener { e ->
                    Timber.w(e, "Failed to persist FCM token to Firestore for uid=%s", uid)
                }
        }
    }

    override fun onDestroy() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        super.onDestroy()
    }

    // ---------- internals ----------

    /** Builds + posts the [NotificationCompat] payload for the given type. */
    private fun postNotification(
        type: NotificationType,
        title: String,
        body: String,
    ) {
        val ctx = this
        val launchIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Forward the type as an extra so MainActivity can deep-link
            // (e.g. open Daily Challenge directly when the user taps a
            // daily_reward push).
            putExtra(EXTRA_NOTIFICATION_TYPE, type.wireKey)
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            type.ordinal,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(ctx, type.channelId)
            .setSmallIcon(type.smallIconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = (System.currentTimeMillis() and 0xFFFFFFL).toInt()
        NotificationManagerCompat.from(ctx).notify(notificationId, builder.build())
        Timber.d("Posted notification type=%s id=%d", type.wireKey, notificationId)
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val FIELD_FCM_TOKEN = "fcmToken"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }
}
