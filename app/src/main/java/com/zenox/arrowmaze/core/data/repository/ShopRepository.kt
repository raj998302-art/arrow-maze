package com.zenox.arrowmaze.core.data.repository

import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.domain.model.Profile
import com.zenox.arrowmaze.core.domain.model.ShopItem
import kotlinx.coroutines.flow.Flow

/**
 * Owns the static shop catalogue + per-player owned items.
 *
 * - [allShopItems] is the full catalogue (~40 items across all categories).
 * - [observeOwned] is the live list of item ids the player owns.
 * - [purchase] performs the coin-deduction (or marks as owned if REAL_MONEY
 *   — the actual Play Billing callback will call back into this method
 *   after the billing flow succeeds).
 * - [equip] flips the per-category equipped flag, ensuring only one item is
 *   equipped per category.
 */
interface ShopRepository {

    val allShopItems: List<ShopItem>

    fun getById(id: String): ShopItem?

    /** Reactive stream of owned item ids. */
    fun observeOwned(): Flow<List<String>>

    /** Reactive stream of equipped (itemId, category) pairs. */
    fun observeEquipped(): Flow<Map<String, String>>

    /**
     * Performs the purchase for [itemId]. Returns the updated [Profile]:
     * - For COINS-priced items, deducts the price from `profile.coins` and
     *   appends `itemId` to `ownedItems` (idempotent). If the player can't
     *   afford it, returns `Result.Failure(Validation)`.
     * - For REAL_MONEY items, returns `Result.Failure(Billing)` — Play
     *   Billing flows must complete first, then the caller re-invokes
     *   [confirmRealMoneyPurchase] to mark the item as owned.
     * - Hint packs add hints to the profile instead of changing cosmetics.
     */
    suspend fun purchase(itemId: String, profile: Profile): Result<Profile>

    /**
     * Marks a REAL_MONEY item as owned after the Play Billing flow succeeds.
     * Returns the updated [Profile] (with the item added to ownedItems).
     */
    suspend fun confirmRealMoneyPurchase(itemId: String, profile: Profile): Result<Profile>

    /** Equips [itemId] (and un-equips every other item in its category). */
    suspend fun equip(itemId: String): Result<Unit>

    /** Returns true if the player owns [itemId]. */
    suspend fun isOwned(itemId: String): Boolean
}
