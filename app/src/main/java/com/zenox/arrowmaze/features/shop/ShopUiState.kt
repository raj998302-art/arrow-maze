package com.zenox.arrowmaze.features.shop

import com.zenox.arrowmaze.core.domain.model.ShopCategory
import com.zenox.arrowmaze.core.domain.model.ShopItem

/**
 * UI state for the Shop screen.
 *
 *  - [Loading] — catalogue + owned + equipped streams are still warming up.
 *  - [Success] — full catalogue grouped by [ShopCategory], plus the player's
 *    owned ids, current coin balance, and per-category equipped item id.
 *  - [Error]   — one of the underlying streams failed (rare; most flows
 *    fall back to empty).
 */
sealed interface ShopUiState {

    data object Loading : ShopUiState

    data class Success(
        val categories: Map<ShopCategory, List<ShopItem>>,
        val owned: Set<String>,
        val coins: Int,
        val equippedByCategory: Map<String, String>,
        /** True while [ShopViewModel.purchase] / [equip] is in flight. */
        val pendingItemId: String? = null,
    ) : ShopUiState

    data class Error(val message: String) : ShopUiState
}

/**
 * One-shot UI events emitted by [ShopViewModel] for the screen to surface as
 * toasts / snackbars / navigations.
 */
sealed interface ShopNavEvent {
    data class ShowToast(val message: String) : ShopNavEvent
    /** Fired after a successful COINS purchase so the caller can navigate back. */
    data object Purchased : ShopNavEvent
    /** Fired after a successful equip. */
    data object Equipped : ShopNavEvent
}
