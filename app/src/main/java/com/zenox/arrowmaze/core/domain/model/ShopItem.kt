package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shop taxonomy. Each item's [ShopItem.category] drives the tab it appears under and the
 * cosmetics slot it occupies when owned.
 */
@Serializable
enum class ShopCategory {
    @SerialName("THEME")             THEME,
    @SerialName("ARROW_SKIN")        ARROW_SKIN,
    @SerialName("TRAIL_FX")          TRAIL_FX,
    @SerialName("BOARD_BACKGROUND")  BOARD_BACKGROUND,
    @SerialName("HINT_PACK")         HINT_PACK,
    @SerialName("COIN_PACK")         COIN_PACK,
    @SerialName("SEASONAL")          SEASONAL,
    @SerialName("PREMIUM_BUNDLE")    PREMIUM_BUNDLE,
    @SerialName("LIMITED_EDITION")   LIMITED_EDITION
}

/** Currency a [ShopItem] is priced in. */
@Serializable
enum class Currency {
    @SerialName("COINS")      COINS,
    @SerialName("REAL_MONEY") REAL_MONEY
}

/** Rarity bucket; controls thumbnail glow colour and drop-rate-weighted distribution. */
@Serializable
enum class Rarity {
    @SerialName("COMMON")    COMMON,
    @SerialName("RARE")      RARE,
    @SerialName("EPIC")      EPIC,
    @SerialName("LEGENDARY") LEGENDARY,
    @SerialName("MYTHIC")    MYTHIC;

    /** Sort weight, lower = more common. */
    val weight: Int get() = ordinal
}

/**
 * A purchasable shop row. The shop catalogue is a static list baked into the binary
 * (Phase 8 will surface it via the UI); the user's owned ids live on [Profile.ownedItems].
 *
 * @property id             Stable sku (matches Play Billing sku when [priceCurrency] is REAL_MONEY).
 * @property title          Short title shown in the tile.
 * @property description    Longer description shown in the detail sheet.
 * @property category       Tab + slot classification.
 * @property price          Numeric price.
 * @property priceCurrency  Whether the price is in soft [Currency.COINS] or [Currency.REAL_MONEY].
 * @property rarity         Rarity tier (drives cosmetic glow + drop rate).
 * @property previewAsset   Asset key for the tile preview.
 * @property isPremium      Reserved for items only available to premium subscribers.
 * @property isLimited      True for time-limited seasonal drops.
 */
@Serializable
data class ShopItem(
    @SerialName("id")            val id: String,
    @SerialName("title")         val title: String,
    @SerialName("description")   val description: String,
    @SerialName("category")      val category: ShopCategory,
    @SerialName("price")         val price: Int,
    @SerialName("priceCurrency") val priceCurrency: Currency,
    @SerialName("rarity")        val rarity: Rarity,
    @SerialName("previewAsset")  val previewAsset: String,
    @SerialName("isPremium")     val isPremium: Boolean = false,
    @SerialName("isLimited")     val isLimited: Boolean = false
)
