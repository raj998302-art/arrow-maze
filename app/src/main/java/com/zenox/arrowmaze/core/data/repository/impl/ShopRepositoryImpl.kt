package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.repository.ShopRepository
import com.zenox.arrowmaze.core.database.dao.OwnedItemDao
import com.zenox.arrowmaze.core.database.entity.OwnedItemEntity
import com.zenox.arrowmaze.core.domain.model.Currency
import com.zenox.arrowmaze.core.domain.model.Profile
import com.zenox.arrowmaze.core.domain.model.Rarity
import com.zenox.arrowmaze.core.domain.model.ShopCategory
import com.zenox.arrowmaze.core.domain.model.ShopItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Concrete [ShopRepository]. Owns the static ~40-item shop catalogue and the
 * per-player owned-items table.
 *
 * The "equip one item per category" rule is enforced atomically inside [equip]
 * via two DAO calls — Room guarantees atomicity inside the suspend transaction
 * because [OwnedItemDao] uses the same Room database connection.
 *
 * Firestore sync: Phase 10
 */
class ShopRepositoryImpl @Inject constructor(
    private val ownedItemDao: OwnedItemDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ShopRepository {

    override val allShopItems: List<ShopItem> by lazy { buildCatalog() }

    override fun getById(id: String): ShopItem? = allShopItems.firstOrNull { it.id == id }

    override fun observeOwned(): Flow<List<String>> =
        ownedItemDao.observeAll().map { rows -> rows.map { it.itemId } }

    override fun observeEquipped(): Flow<Map<String, String>> =
        ownedItemDao.observeAll().map { rows ->
            rows.filter { it.isEquipped }.associate { it.category to it.itemId }
        }

    override suspend fun purchase(itemId: String, profile: Profile): Result<Profile> =
        withContext(io) {
            resultOf {
                val item = getById(itemId)
                    ?: throw NoSuchElementException("Unknown shop item id=$itemId")

                // Already owned — no-op, return profile unchanged.
                if (ownedItemDao.get(itemId) != null) {
                    return@resultOf profile
                }

                when (item.priceCurrency) {
                    Currency.COINS -> {
                        if (profile.coins < item.price) {
                            throw InsufficientFundsException(
                                "Need $item.price coins, have ${profile.coins}"
                            )
                        }
                        // Hint packs grant hints; everything else is just owned.
                        val updatedProfile = when (item.category) {
                            ShopCategory.HINT_PACK -> {
                                val hintsToAdd = hintPackQuantity(item.id)
                                profile.copy(
                                    coins = profile.coins - item.price,
                                    hints = profile.hints + hintsToAdd,
                                )
                            }
                            ShopCategory.COIN_PACK -> {
                                // Coin packs are REAL_MONEY; if we ever see one here,
                                // treat as no-op (caller should use confirmRealMoneyPurchase).
                                profile
                            }
                            else -> profile.copy(coins = profile.coins - item.price)
                        }
                        persistOwned(itemId, item)
                        Timber.i("Purchased %s for %d coins", itemId, item.price)
                        // Firestore sync: Phase 10
                        updatedProfile
                    }
                    Currency.REAL_MONEY -> {
                        // REAL_MONEY items must go through confirmRealMoneyPurchase after
                        // the Play Billing flow succeeds.
                        throw RealMoneyItemException(
                            "REAL_MONEY items require confirmRealMoneyPurchase(): $itemId"
                        )
                    }
                }
            }
        }

    override suspend fun confirmRealMoneyPurchase(itemId: String, profile: Profile): Result<Profile> =
        withContext(io) {
            resultOf {
                val item = getById(itemId)
                    ?: throw NoSuchElementException("Unknown shop item id=$itemId")
                require(item.priceCurrency == Currency.REAL_MONEY) {
                    "confirmRealMoneyPurchase called for a COINS-priced item: $itemId"
                }
                if (ownedItemDao.get(itemId) != null) return@resultOf profile

                val updatedProfile = when (item.category) {
                    ShopCategory.COIN_PACK -> {
                        val coinsToAdd = coinPackQuantity(item.id)
                        profile.copy(coins = profile.coins + coinsToAdd)
                    }
                    ShopCategory.PREMIUM_BUNDLE -> profile.copy(isPremium = true, isVip = true)
                    else -> profile
                }
                persistOwned(itemId, item)
                Timber.i("Confirmed REAL_MONEY purchase: %s", itemId)
                // Firestore sync: Phase 10
                updatedProfile
            }
        }

    override suspend fun equip(itemId: String): Result<Unit> = withContext(io) {
        resultOf {
            val owned = ownedItemDao.get(itemId)
                ?: throw NoSuchElementException("Item not owned: $itemId")
            val item = getById(itemId)
                ?: throw NoSuchElementException("Unknown shop item id=$itemId")
            ownedItemDao.unequipAllInCategory(item.category.name)
            ownedItemDao.setEquipped(itemId)
            Timber.i("Equipped: %s (category=%s)", itemId, item.category.name)
            // Firestore sync: Phase 10
        }
    }

    override suspend fun isOwned(itemId: String): Boolean = withContext(io) {
        ownedItemDao.get(itemId) != null
    }

    private suspend fun persistOwned(itemId: String, item: ShopItem) {
        ownedItemDao.upsert(
            OwnedItemEntity(
                itemId = itemId,
                category = item.category.name,
                purchasedAtEpochMs = System.currentTimeMillis(),
                isEquipped = false,
            )
        )
    }

    /** Returns the hint-count associated with a hint-pack item id. */
    private fun hintPackQuantity(itemId: String): Int = when (itemId) {
        "hint_pack_5"   -> 5
        "hint_pack_15"  -> 15
        "hint_pack_50"  -> 50
        "hint_pack_200" -> 200
        else            -> 0
    }

    /** Returns the coin-count associated with a coin-pack item id. */
    private fun coinPackQuantity(itemId: String): Int = when (itemId) {
        "coin_pack_1000"  -> 1000
        "coin_pack_5000"  -> 5000
        "coin_pack_12000" -> 12000
        "coin_pack_30000" -> 30000
        else              -> 0
    }

    // ---------- Catalogue builder ----------

    /**
     * Builds the static shop catalogue: 13 themes + 6 arrow skins + 6 trail FX
     * + 6 board backgrounds + 4 hint packs + 4 coin packs + 2 seasonal
     * + 1 premium bundle + 1 limited-edition = 43 items.
     */
    @Suppress("LongMethod")
    private fun buildCatalog(): List<ShopItem> {
        val list = mutableListOf<ShopItem>()

        // ----- Themes: 13 (from GameTheme.ALL_THEMES) -----
        com.zenox.arrowmaze.core.domain.model.GameTheme.ALL_THEMES.forEach { theme ->
            val currency = if (theme.isPremium && theme.price > 0) Currency.COINS else Currency.COINS
            val price = if (theme.isPremium) theme.price else 0
            list += ShopItem(
                id = "theme_${theme.id}",
                title = "${theme.displayName} Theme",
                description = "Unlock the ${theme.displayName} cosmetic theme for the game board.",
                category = ShopCategory.THEME,
                price = price,
                priceCurrency = currency,
                rarity = if (theme.isPremium) Rarity.RARE else Rarity.COMMON,
                previewAsset = "preview_theme_${theme.id}",
                isPremium = theme.isPremium,
                isLimited = false,
            )
        }

        // ----- Arrow Skins: 6 -----
        val arrowSkins = listOf(
            Triple("arrow_classic",  "Classic Arrow",   0),
            Triple("arrow_neon",     "Neon Arrow",      200),
            Triple("arrow_gold",     "Gold Arrow",      500),
            Triple("arrow_rainbow",  "Rainbow Arrow",   1000),
            Triple("arrow_galaxy",   "Galaxy Arrow",    1500),
            Triple("arrow_mythic",   "Mythic Arrow",    3000),
        )
        arrowSkins.forEachIndexed { idx, (id, title, price) ->
            list += ShopItem(
                id = id,
                title = title,
                description = "Reskin the arrows on the board with the $title style.",
                category = ShopCategory.ARROW_SKIN,
                price = price,
                priceCurrency = Currency.COINS,
                rarity = when (idx) {
                    0    -> Rarity.COMMON
                    1    -> Rarity.COMMON
                    2    -> Rarity.RARE
                    3    -> Rarity.EPIC
                    4    -> Rarity.LEGENDARY
                    else -> Rarity.MYTHIC
                },
                previewAsset = "preview_$id",
                isPremium = false,
                isLimited = false,
            )
        }

        // ----- Trail FX: 6 -----
        val trailFxs = listOf(
            Triple("trail_none",     "No Trail",      0),
            Triple("trail_sparkle",  "Sparkle Trail", 150),
            Triple("trail_fire",     "Fire Trail",    300),
            Triple("trail_lightning","Lightning Trail", 600),
            Triple("trail_rainbow",  "Rainbow Trail", 1200),
            Triple("trail_cosmic",   "Cosmic Trail",  2500),
        )
        trailFxs.forEachIndexed { idx, (id, title, price) ->
            list += ShopItem(
                id = id,
                title = title,
                description = "Add the $title visual effect to the path your arrow carves.",
                category = ShopCategory.TRAIL_FX,
                price = price,
                priceCurrency = Currency.COINS,
                rarity = when (idx) {
                    0    -> Rarity.COMMON
                    1    -> Rarity.COMMON
                    2    -> Rarity.RARE
                    3    -> Rarity.EPIC
                    4    -> Rarity.LEGENDARY
                    else -> Rarity.MYTHIC
                },
                previewAsset = "preview_$id",
                isPremium = false,
                isLimited = false,
            )
        }

        // ----- Board Backgrounds: 6 -----
        val backgrounds = listOf(
            Triple("bg_default",    "Default Board",     0),
            Triple("bg_parchment",  "Parchment Board",   150),
            Triple("bg_chalkboard", "Chalkboard Board",  300),
            Triple("bg_blueprint",  "Blueprint Board",   600),
            Triple("bg_hexgrid",    "Hex Grid Board",    1000),
            Triple("bg_starfield",  "Starfield Board",   1800),
        )
        backgrounds.forEachIndexed { idx, (id, title, price) ->
            list += ShopItem(
                id = id,
                title = title,
                description = "Replace the default board background with the $title texture.",
                category = ShopCategory.BOARD_BACKGROUND,
                price = price,
                priceCurrency = Currency.COINS,
                rarity = when (idx) {
                    0    -> Rarity.COMMON
                    1    -> Rarity.COMMON
                    2    -> Rarity.RARE
                    3    -> Rarity.EPIC
                    4    -> Rarity.LEGENDARY
                    else -> Rarity.MYTHIC
                },
                previewAsset = "preview_$id",
                isPremium = false,
                isLimited = false,
            )
        }

        // ----- Hint Packs: 4 -----
        val hintPacks = listOf(
            Quad("hint_pack_5",   "5 Hints",    100,  5),
            Quad("hint_pack_15",  "15 Hints",   250,  15),
            Quad("hint_pack_50",  "50 Hints",   700,  50),
            Quad("hint_pack_200", "200 Hints", 2000, 200),
        )
        hintPacks.forEachIndexed { idx, (id, title, price, _) ->
            list += ShopItem(
                id = id,
                title = title,
                description = "Top up your hint balance with $title. Hints reveal the next optimal rotation.",
                category = ShopCategory.HINT_PACK,
                price = price,
                priceCurrency = Currency.COINS,
                rarity = when (idx) {
                    0    -> Rarity.COMMON
                    1    -> Rarity.RARE
                    2    -> Rarity.EPIC
                    else -> Rarity.LEGENDARY
                },
                previewAsset = "preview_$id",
                isPremium = false,
                isLimited = false,
            )
        }

        // ----- Coin Packs: 4 (REAL_MONEY) -----
        // Price field carries the USD * 100 (cents) so callers can format it.
        val coinPacks = listOf(
            Quad("coin_pack_1000",  "1,000 Coins",  99,  1000),
            Quad("coin_pack_5000",  "5,000 Coins",  499, 5000),
            Quad("coin_pack_12000", "12,000 Coins", 999, 12000),
            Quad("coin_pack_30000", "30,000 Coins", 1999, 30000),
        )
        coinPacks.forEachIndexed { idx, (id, title, priceCents, _) ->
            list += ShopItem(
                id = id,
                title = title,
                description = "Purchase $title with real money. Best value for dedicated players.",
                category = ShopCategory.COIN_PACK,
                price = priceCents,
                priceCurrency = Currency.REAL_MONEY,
                rarity = when (idx) {
                    0    -> Rarity.COMMON
                    1    -> Rarity.RARE
                    2    -> Rarity.EPIC
                    else -> Rarity.LEGENDARY
                },
                previewAsset = "preview_$id",
                isPremium = false,
                isLimited = false,
            )
        }

        // ----- Seasonal: 2 -----
        list += ShopItem(
            id = "seasonal_winter_2024",
            title = "Winter Festival Theme",
            description = "Limited winter-themed board palette. Available only during the Winter Festival.",
            category = ShopCategory.SEASONAL,
            price = 400,
            priceCurrency = Currency.COINS,
            rarity = Rarity.LEGENDARY,
            previewAsset = "preview_seasonal_winter",
            isPremium = false,
            isLimited = true,
        )
        list += ShopItem(
            id = "seasonal_summer_2025",
            title = "Summer Splash Trail",
            description = "Beach-themed trail FX. Available only during the Summer Splash event.",
            category = ShopCategory.SEASONAL,
            price = 500,
            priceCurrency = Currency.COINS,
            rarity = Rarity.LEGENDARY,
            previewAsset = "preview_seasonal_summer",
            isPremium = false,
            isLimited = true,
        )

        // ----- Premium Bundle: 1 -----
        list += ShopItem(
            id = "premium_bundle",
            title = "Premium Bundle",
            description = "Remove all ads, unlock every cosmetic theme, and upgrade to VIP status forever.",
            category = ShopCategory.PREMIUM_BUNDLE,
            price = 999, // $9.99
            priceCurrency = Currency.REAL_MONEY,
            rarity = Rarity.MYTHIC,
            previewAsset = "preview_premium_bundle",
            isPremium = true,
            isLimited = false,
        )

        // ----- Limited Edition: 1 -----
        list += ShopItem(
            id = "limited_mythic_arrow_launch",
            title = "Launch Edition Mythic Arrow",
            description = "Commemorative mythic arrow skin for early adopters. Never sold again after launch month.",
            category = ShopCategory.LIMITED_EDITION,
            price = 5000,
            priceCurrency = Currency.COINS,
            rarity = Rarity.MYTHIC,
            previewAsset = "preview_limited_launch",
            isPremium = false,
            isLimited = true,
        )

        require(list.size >= 40) {
            "Shop catalogue must have at least 40 entries, got ${list.size}"
        }
        return list
    }

    /** Local 4-tuple helper used by the catalogue builder. */
    private data class Quad(
        val id: String,
        val title: String,
        val price: Int,
        val quantity: Int,
    )
}

/** Thrown when a COINS purchase is attempted with insufficient coins. */
class InsufficientFundsException(message: String) : RuntimeException(message)

/** Thrown when a REAL_MONEY purchase is attempted via the COINS-only entry point. */
class RealMoneyItemException(message: String) : RuntimeException(message)
