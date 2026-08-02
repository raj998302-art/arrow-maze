package com.zenox.arrowmaze.features.shop

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.data.repository.ShopRepository
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.ShopItem
import com.zenox.arrowmaze.features.shop.components.ItemAction
import com.zenox.arrowmaze.features.shop.components.ItemPreview
import com.zenox.arrowmaze.features.shop.components.RarityBadge
import com.zenox.arrowmaze.features.shop.components.isCosmeticCategory
import com.zenox.arrowmaze.features.shop.components.resolveItemAction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

/**
 * Dedicated ViewModel for the ShopItem detail screen.
 *
 * Reads the [itemId] from its [SavedStateHandle] so the detail screen can be
 * reached via `Destination.ShopItem.build(itemId)`. Mirrors the reactive
 * surface of [ShopViewModel] (catalogue + owned + equipped + coins + pending)
 * without needing the grid VM to share state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShopItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shopRepository: ShopRepository,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val themeManager: com.zenox.arrowmaze.core.designsystem.theme.ThemeManager,
) : ViewModel() {

    val itemId: String = savedStateHandle.get<String>("itemId")
        ?: throw IllegalStateException("ShopItemDetailScreen requires an itemId nav arg")

    private val _pendingItemId = MutableStateFlow<String?>(null)
    val pendingItemId: StateFlow<String?> = _pendingItemId.asStateFlow()

    private val _navEvents = Channel<ShopNavEvent>(capacity = Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

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
                            categories = shopRepository.allShopItems.groupBy { it.category },
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
            emit(ShopUiState.Error(t.message ?: "Failed to load item"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ShopUiState.Loading,
        )

    fun purchase(itemId: String) {
        val item = shopRepository.getById(itemId) ?: return
        if (item.priceCurrency == com.zenox.arrowmaze.core.domain.model.Currency.REAL_MONEY) {
            viewModelScope.launch {
                _navEvents.send(
                    ShopNavEvent.ShowToast("Google Play Billing integrated in Phase 10"),
                )
            }
            return
        }
        viewModelScope.launch {
            _pendingItemId.value = itemId
            val uid = sessionRepository.currentUidFlow.first() ?: return@launch
            val profile = (profileRepository.getProfile(uid) as? com.zenox.arrowmaze.core.common.Result.Success)?.data
                ?: run {
                    _pendingItemId.value = null
                    return@launch
                }
            shopRepository.purchase(itemId, profile)
                .onSuccess { updated ->
                    profileRepository.saveProfile(updated)
                    _navEvents.send(ShopNavEvent.Purchased)
                    _navEvents.send(ShopNavEvent.ShowToast("${item.title} unlocked!"))
                }
                .onFailure { error: AppError ->
                    _navEvents.send(ShopNavEvent.ShowToast(error.message))
                }
            _pendingItemId.value = null
        }
    }

    fun equip(itemId: String) {
        val item = shopRepository.getById(itemId) ?: return
        if (item.category == com.zenox.arrowmaze.core.domain.model.ShopCategory.HINT_PACK ||
            item.category == com.zenox.arrowmaze.core.domain.model.ShopCategory.COIN_PACK) return
        viewModelScope.launch {
            _pendingItemId.value = itemId
            shopRepository.equip(itemId)
                .onSuccess {
                    val uid = sessionRepository.currentUidFlow.first()
                    if (uid != null) {
                        val profile = (profileRepository.getProfile(uid) as? com.zenox.arrowmaze.core.common.Result.Success)?.data
                        if (profile != null) {
                            val themeId = itemId.removePrefix("theme_")
                            val updated = when (item.category) {
                                com.zenox.arrowmaze.core.domain.model.ShopCategory.THEME ->
                                    profile.copy(currentThemeId = themeId)
                                com.zenox.arrowmaze.core.domain.model.ShopCategory.ARROW_SKIN ->
                                    profile.copy(currentArrowSkinId = itemId)
                                com.zenox.arrowmaze.core.domain.model.ShopCategory.TRAIL_FX ->
                                    profile.copy(currentTrailFxId = itemId)
                                else -> profile
                            }
                            profileRepository.saveProfile(updated)
                            if (item.category == com.zenox.arrowmaze.core.domain.model.ShopCategory.THEME) {
                                themeManager.setTheme(themeId)
                            }
                        }
                    }
                    _navEvents.send(ShopNavEvent.Equipped)
                    _navEvents.send(ShopNavEvent.ShowToast("${item.title} equipped"))
                }
                .onFailure { error: AppError ->
                    _navEvents.send(ShopNavEvent.ShowToast(error.message))
                }
            _pendingItemId.value = null
        }
    }

    fun getItem(id: String): ShopItem? = shopRepository.getById(id)
}

/**
 * Full-screen item detail. Large preview, title, description, rarity, price,
 * and a Buy/Equip/Owned button. Reads the `itemId` from the
 * [ShopItemDetailViewModel] (which got it from the [SavedStateHandle] wired
 * up by the NavHost's `navArgument("itemId")`).
 *
 * @param onBack Called when the user taps the back arrow.
 * @param onPurchased Called after a successful purchase (typically
 *   `navController.popBackStack()`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopItemDetailScreen(
    viewModel: ShopItemDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPurchased: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingItemId by viewModel.pendingItemId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is ShopNavEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                ShopNavEvent.Purchased -> onPurchased()
                ShopNavEvent.Equipped -> { /* UI auto-refreshes */ }
            }
        }
    }

    val item: ShopItem? = remember(viewModel.itemId) { viewModel.getItem(viewModel.itemId) }
    val success = uiState as? ShopUiState.Success

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = item?.title ?: "Item",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    ArrowMazeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            when {
                uiState is ShopUiState.Loading -> LoadingState()
                uiState is ShopUiState.Error -> ErrorState(
                    message = (uiState as ShopUiState.Error).message,
                    onRetry = onBack,
                )
                item == null -> ErrorState(
                    message = "Item not found.",
                    onRetry = onBack,
                )
                else -> DetailContent(
                    item = item,
                    state = success,
                    pending = pendingItemId == item.id,
                    onPurchase = { viewModel.purchase(item.id) },
                    onEquip = { viewModel.equip(item.id) },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    item: ShopItem,
    state: ShopUiState.Success?,
    pending: Boolean,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isOwned = state?.owned?.contains(item.id) == true
    val equippedId = state?.equippedByCategory?.get(item.category.name)
    val isEquipped = equippedId == item.id
    val canAfford = (state?.coins ?: 0) >= item.price
    val isCosmetic = isCosmeticCategory(item.category)
    val action = resolveItemAction(
        isOwned = isOwned,
        isEquipped = isEquipped,
        pending = pending,
        isCosmetic = isCosmetic,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SpacingTokens.lg),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.lg),
    ) {
        // Large preview (4:3)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cs.surface,
            tonalElevation = ElevationTokens.Level1,
            shadowElevation = ElevationTokens.Level2,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                ItemPreview(item = item, modifier = Modifier.fillMaxSize())
            }
        }

        // Title + rarity
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(SpacingTokens.sm))
            RarityBadge(rarity = item.rarity)
        }

        // Description
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )

        // Price row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = cs.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier.padding(SpacingTokens.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Price",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                PriceLabel(item = item, canAfford = canAfford)
            }
        }

        // Action button
        when (action) {
            ItemAction.PROCESSING -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            ItemAction.OWNED -> ArrowMazeButton(
                text = "Owned",
                onClick = {},
                style = ButtonStyle.Outline,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
            ItemAction.EQUIPPED -> ArrowMazeButton(
                text = "Equipped",
                onClick = {},
                style = ButtonStyle.Tonal,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
            ItemAction.EQUIP -> ArrowMazeButton(
                text = "Equip",
                onClick = onEquip,
                style = ButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            ItemAction.BUY_COINS -> ArrowMazeButton(
                text = if (canAfford) "Buy" else "Not enough coins",
                onClick = onPurchase,
                style = ButtonStyle.Primary,
                enabled = canAfford,
                modifier = Modifier.fillMaxWidth(),
            )
            ItemAction.BUY_MONEY -> ArrowMazeButton(
                text = "Buy",
                onClick = onPurchase,
                style = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(SpacingTokens.sm))
    }
}

@Composable
private fun PriceLabel(
    item: ShopItem,
    canAfford: Boolean,
) {
    val cs = MaterialTheme.colorScheme
    when (item.priceCurrency) {
        com.zenox.arrowmaze.core.domain.model.Currency.COINS -> {
            if (item.price == 0) {
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = cs.tertiary,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons.Coin,
                        contentDescription = null,
                        tint = if (canAfford) cs.tertiary else cs.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(SpacingTokens.xs))
                    Text(
                        text = item.price.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (canAfford) cs.onSurface else cs.error,
                    )
                }
            }
        }
        com.zenox.arrowmaze.core.domain.model.Currency.REAL_MONEY -> {
            val dollars = item.price / 100.0
            Text(
                text = "$%.2f".format(dollars),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = cs.tertiary,
            )
        }
    }
}
