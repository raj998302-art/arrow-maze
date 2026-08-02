package com.zenox.arrowmaze.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Firestore-facing achievement-progress DTO. Stored under
 * `users/{uid}/achievement_progress/{achievementId}` so each achievement's
 * progress can be synced independently.
 */
@Serializable
data class AchievementProgressDto(
    @SerialName("achievement_id")    val achievementId: String,
    @SerialName("unlocked")          val unlocked: Boolean,
    @SerialName("unlocked_at")       val unlockedAt: Long? = null,
    @SerialName("progress")          val progress: Int,
)
