package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.Profile
import kotlinx.coroutines.flow.Flow

/**
 * Reads / writes the player's [Profile]. Offline-first: every write hits Room
 * immediately; Firestore sync is best-effort in the background.
 *
 * The repository hides the uid from most callers (the SessionRepository is
 * the source of truth for "who is the current user") so the methods accept
 * it explicitly to keep the surface simple and stateless.
 */
interface ProfileRepository {

    /** One-shot read of the profile for [uid]. Returns `Result.Failure(NotFound)` if missing. */
    suspend fun getProfile(uid: String): Result<Profile>

    /** Reactive observation of the profile for [uid]; emits `null` if not present. */
    fun observeProfile(uid: String): Flow<Profile?>

    /** Full-replace write of [profile] to Room (and best-effort Firestore). */
    suspend fun saveProfile(profile: Profile): Result<Unit>

    /** Targeted update of the economy fields (coins / hints / lives). */
    suspend fun updateEconomy(uid: String, coins: Int, hints: Int, lives: Int): Result<Unit>

    /** Targeted update of the progression fields (level / xp). */
    suspend fun updateProgress(uid: String, level: Int, xp: Int): Result<Unit>

    /** Updates the three equipped cosmetic slots atomically. */
    suspend fun updateEquippedCosmetics(
        uid: String,
        themeId: String,
        arrowSkinId: String,
        trailFxId: String,
    ): Result<Unit>

    /** Adds an owned item id to the profile's ownedItems list (idempotent). */
    suspend fun addOwnedItem(uid: String, itemId: String): Result<Unit>

    /** Adds an unlocked achievement id to the profile (idempotent). */
    suspend fun addUnlockedAchievement(uid: String, achievementId: String): Result<Unit>

    /**
     * Merges a guest profile into a freshly-created account: copies guest
     * coins / hints / level / xp / ownedItems onto the account profile, then
     * deletes the guest row. Best-effort Firestore sync at the end.
     */
    suspend fun mergeGuestIntoAccount(guestProfile: Profile, accountUid: String): Result<Unit>
}
