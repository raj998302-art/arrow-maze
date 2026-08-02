package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.OwnedItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [OwnedItemEntity]. The equip-one-per-category invariant is
 * enforced by [unequipAllInCategory] followed by [setEquipped]; callers wrap
 * both in a Room transaction (via the repository).
 */
@Dao
interface OwnedItemDao {

    @Upsert
    suspend fun upsert(entity: OwnedItemEntity)

    @Query("SELECT * FROM owned_items WHERE itemId = :itemId")
    suspend fun get(itemId: String): OwnedItemEntity?

    @Query("SELECT * FROM owned_items")
    fun observeAll(): Flow<List<OwnedItemEntity>>

    @Query("SELECT * FROM owned_items")
    suspend fun getAll(): List<OwnedItemEntity>

    @Query("SELECT * FROM owned_items WHERE category = :category AND isEquipped = 1 LIMIT 1")
    suspend fun getEquippedInCategory(category: String): OwnedItemEntity?

    @Query("UPDATE owned_items SET isEquipped = 0 WHERE category = :category")
    suspend fun unequipAllInCategory(category: String)

    @Query("UPDATE owned_items SET isEquipped = 1 WHERE itemId = :itemId")
    suspend fun setEquipped(itemId: String)

    @Query("DELETE FROM owned_items WHERE itemId = :itemId")
    suspend fun delete(itemId: String)
}
