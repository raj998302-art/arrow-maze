package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Owned shop item. The shop catalogue itself is static (see ShopRepositoryImpl);
 * this entity tracks which items the player has purchased and which is currently
 * equipped. The [category] column allows the "equip one per category" rule to
 * be enforced with a single SQL update (see OwnedItemDao).
 */
@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "itemId")            val itemId: String,
    @ColumnInfo(name = "category")          val category: String,
    @ColumnInfo(name = "purchasedAtEpochMs") val purchasedAtEpochMs: Long,
    @ColumnInfo(name = "isEquipped")        val isEquipped: Boolean,
)
