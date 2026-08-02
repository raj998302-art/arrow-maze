package com.zenox.arrowmaze.features.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.ShopRepository
import com.zenox.arrowmaze.core.designsystem.theme.ThemeManager
import com.zenox.arrowmaze.core.domain.model.Currency
import com.zenox.arrowmaze.core.domain.model.Profile
import com.zenox.arrowmaze.core.domain.model.ShopCategory
import com.zenox.arrowmaze.core.domain.model.ShopItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Shop + ShopItemDetail screens.
 *
 * Reactive input streams:
 *  - [SessionRepository.currentUidFlow] → [ProfileRepository.observeProfile] (for the
 *    live coin balance + owned/equipped ids on the profile row),
 *  - [ShopRepository.observeOwned] (per-item owned flag),
 *  - [ShopRepository.observeEquipped] (per-category equipped-item id),
 *  - [ShopRepository.allShopItems] (the static catalogue; grouped into
 *    `Map<ShopCategory, List<ShopItem>>` for the tab + grid layout).
 *
 * Mutation surface:
 *  - [purchase] — coin-prices items deduct coins + add to owned; REAL_MONEY
 *    items emit a `ShowToast("Google Play Billing integrated in Phase 10")`
 *    event and short-circuit (Phase 10 wires the real billing flow).
 *  - [equip]    — only callable on already-owned items; flips the per-category
 *    equipped flag atomically and persists the new cosmetics on the profile.
 *    For THEME items, also calls [ThemeManager.setTheme] so the app re-themes
 *    immediately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val themeManager: ThemeManager,
) : ViewModel() {

    private val _pendingItemId = MutableStateFlow<String?>(null)
    val pendingItemId: StateFlow<String?> = _pendingItemId.asStateFlow()

    private val _navEvents = Channel<ShopNavEvent>(capacity = Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    /**
     * Hot UI state. Derived from the uid flow → flat-mapped onto
     * (profile, owned, equipped). The static catalogue is grouped once per
     * emission (cheap; 43 items) so the screen can look up items by category
     * without doing the grouping itself.
     */
    val uiState: StateFlow<ShopUiState> = sessionRepository.currentUidFlow
        .flatMapLatest { uid ->
            if (uid == null) {
                kotlinx.coroutines.flow.flowOf<ShopUiState>(ShopUiState.Error("Not signed in"))
            } else {
                combine(
                    profileRepository.observeProfile(uid),
                    shopRepository.observeOwned(),
                    shopRepository.observeEquipped(),
                    _pendingItemId,
                ) { profile, owned, equipped, pending ->
                    if (profile == null) {
                        ShopUiState.Loading
                    } else {
                        ShopUiState.Success(
                            categories = groupByCategory(shopRepository.allShopItems),
                            owned = owned.toSet(),
                            coins = profile.coins,
                            equippedByCategory = equipped,
                            pendingItemId = pending,
                        )
                    }
                }
            }
        }
        .catch { t ->
            Timber.e(t, "Shop stream failed")
            emit(ShopUiState.Error(t.message ?: "Failed to load shop"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ShopUiState.Loading,
        )

    /** Snapshot of the current profile (used by [purchase] / [equip]). */
    private suspend fun currentProfile(): Profile? {
        val uid = sessionRepository.currentUidFlow.first() ?: return null
        val result = profileRepository.getProfile(uid)
        return (result as? Result.Success)?.data
    }

    /**
     * Attempts to purchase [itemId]:
     *  - For COINS items: deducts coins + adds to owned.
     *  - For REAL_MONEY items: emits a "Billing coming soon" toast and
     *    short-circuits (Phase 10 wires the real billing flow).
     */
    fun purchase(itemId: String) {
        val item = shopRepository.getById(itemId) ?: run {
            viewModelScope.launch { _navEvents.send(ShopNavEvent.ShowToast("Unknown item")) }
            return
        }
        if (item.priceCurrency == Currency.REAL_MONEY) {
            viewModelScope.launch {
                _navEvents.send(
                    ShopNavEvent.ShowToast("Google Play Billing integrated in Phase 10"),
                )
            }
            return
        }
        viewModelScope.launch {
            _pendingItemId.value = itemId
            val profile = currentProfile()
            if (profile == null) {
                _pendingItemId.value = null
                _navEvents.send(ShopNavEvent.ShowToast("Sign in to make purchases"))
                return@launch
            }
            shopRepository.purchase(itemId, profile)
                .onSuccess { updated ->
                    profileRepository.saveProfile(updated)
                    _navEvents.send(ShopNavEvent.Purchased)
                    _navEvents.send(ShopNavEvent.ShowToast("${item.title} unlocked!"))
                }
                .onFailure { error: AppError ->
                    Timber.w(error.asException(), "Purchase failed: %s", itemId)
                    _navEvents.send(ShopNavEvent.ShowToast(error.message))
                }
            _pendingItemId.value = null
        }
    }

    /**
     * Equips [itemId]. Only valid for owned items in the THEME / ARROW_SKIN /
     * TRAIL_FX / BOARD_BACKGROUND categories — for HINT_PACK and COIN_PACK
     * the call is a no-op.
     */
    fun equip(itemId: String) {
        val item = shopRepository.getById(itemId) ?: return
        if (item.category == ShopCategory.HINT_PACK || item.category == ShopCategory.COIN_PACK) return
        viewModelScope.launch {
            _pendingItemId.value = itemId
            shopRepository.equip(itemId)
                .onSuccess {
                    // Persist the new equipped cosmetic on the profile so the
                    // profile screen + the in-game canvas reflect it.
                    val profile = currentProfile()
                    if (profile != null) {
                        val themeId = themeIdFromItem(itemId)
                        val updated = when (item.category) {
                            ShopCategory.THEME -> profile.copy(currentThemeId = themeId)
                            ShopCategory.ARROW_SKIN -> profile.copy(currentArrowSkinId = itemId)
                            ShopCategory.TRAIL_FX -> profile.copy(currentTrailFxId = itemId)
                            else -> profile
                        }
                        profileRepository.saveProfile(updated)
                        // For theme items, also push the theme id into the
                        // SettingsRepository (and through ThemeManager) so the
                        // whole app re-themes immediately.
                        if (item.category == ShopCategory.THEME) {
                            themeManager.setTheme(themeId)
                        }
                    }
                    _navEvents.send(ShopNavEvent.Equipped)
                    _navEvents.send(ShopNavEvent.ShowToast("${item.title} equipped"))
                }
                .onFailure { error: AppError ->
                    Timber.w(error.asException(), "Equip failed: %s", itemId)
                    _navEvents.send(ShopNavEvent.ShowToast(error.message))
                }
            _pendingItemId.value = null
        }
    }

    /** Returns true if [itemId] is owned by the current player. */
    fun isOwned(itemId: String): Boolean {
        val state = uiState.value as? ShopUiState.Success ?: return false
        return itemId in state.owned
    }

    /** Returns the equipped item id for [category] (or null). */
    fun equippedIdFor(category: ShopCategory): String? {
        val state = uiState.value as? ShopUiState.Success ?: return null
        return state.equippedByCategory[category.name]
    }

    /** Looks up an item by id; null if not in the catalogue. */
    fun getItem(itemId: String): ShopItem? = shopRepository.getById(itemId)

    /** Groups the flat catalogue into a category-keyed map (sorted by rarity). */
    private fun groupByCategory(items: List<ShopItem>): Map<ShopCategory, List<ShopItem>> =
        items.groupBy { it.category }
            .mapValues { (_, list) -> list.sortedBy { it.rarity.weight } }

    /** Extracts the underlying theme id from a `theme_xxx` shop item id. */
    private fun themeIdFromItem(itemId: String): String =
        itemId.removePrefix("theme_")
}
