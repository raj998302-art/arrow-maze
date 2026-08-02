package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

/**
 * Owns the static achievement catalogue + per-player progress.
 *
 * Definitions (title / requirement / rewards) come from [allAchievements] — a
 * hard-coded list baked into the binary. Per-player progress is persisted in
 * Room's `achievement_progress` table and synced to Firestore best-effort.
 */
interface AchievementRepository {

    /** Full static catalogue. ~100 achievements across all categories. */
    val allAchievements: List<Achievement>

    /** Lookup by id. */
    fun getById(id: String): Achievement?

    /** Reactive stream of currently-unlocked achievement ids. */
    fun observeUnlocked(): Flow<List<String>>

    /** Reactive stream of progress for every achievement in the catalogue. */
    fun observeAllProgress(): Flow<Map<String, Int>>

    /** One-shot read of progress for [achievementId]; 0 if no row exists. */
    suspend fun getProgress(achievementId: String): Int

    /**
     * Sets the integer progress for [achievementId]. The achievements engine
     * calls this after evaluating the requirement against the live profile.
     * If [progress] crosses the requirement threshold, the achievement is
     * auto-unlocked.
     */
    suspend fun setProgress(achievementId: String, progress: Int): Result<Unit>

    /** Force-unlocks an achievement (e.g. for admin grants or special events). */
    suspend fun unlock(achievementId: String): Result<Unit>

    /** True if the achievement is currently unlocked. */
    suspend fun isUnlocked(achievementId: String): Boolean
}
