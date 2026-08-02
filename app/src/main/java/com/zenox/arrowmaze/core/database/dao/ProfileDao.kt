package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [ProfileEntity]. Economy / progress updates use targeted
 * `UPDATE` statements to avoid serialising the full profile on every coin gain.
 */
@Dao
interface ProfileDao {

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE uid = :uid")
    suspend fun get(uid: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE uid = :uid")
    fun observe(uid: String): Flow<ProfileEntity?>

    @Query("UPDATE profiles SET coins = :coins, hints = :hints, lives = :lives WHERE uid = :uid")
    suspend fun updateEconomy(uid: String, coins: Int, hints: Int, lives: Int)

    @Query("UPDATE profiles SET level = :level, xp = :xp WHERE uid = :uid")
    suspend fun updateProgress(uid: String, level: Int, xp: Int)

    @Query("UPDATE profiles SET currentThemeId = :themeId, currentArrowSkinId = :arrowSkinId, currentTrailFxId = :trailFxId WHERE uid = :uid")
    suspend fun updateEquippedCosmetics(uid: String, themeId: String, arrowSkinId: String, trailFxId: String)

    @Query("UPDATE profiles SET ownedItems = :ownedItemsJson WHERE uid = :uid")
    suspend fun updateOwnedItems(uid: String, ownedItemsJson: String)

    @Query("DELETE FROM profiles WHERE uid = :uid")
    suspend fun delete(uid: String)
}
