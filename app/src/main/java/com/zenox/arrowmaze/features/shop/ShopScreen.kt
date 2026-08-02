package com.zenox.arrowmaze.features.shop

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.CoinCounter
import com.zenox.arrowmaze.core.designsystem.components.CoinCounterSize
import com.zenox.arrowmaze.core.designsystem.components.EmptyState
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.ShopCategory
import com.zenox.arrowmaze.core.domain.model.ShopItem
import com.zenox.arrowmaze.features.shop.components.CategoryTab
import com.zenox.arrowmaze.features.shop.components.ItemAction
import com.zenox.arrowmaze.features.shop.components.ShopItemCard
import com.zenox.arrowmaze.features.shop.components.isCosmeticCategory
import com.zenox.arrowmaze.features.shop.components.resolveItemAction

/**
 * Display metadata for a shop category tab.
 */
private data class ShopTab(val category: ShopCategory, val label: String)

private val shopTabs: List<ShopTab> = listOf(
    ShopTab(ShopCategory.THEME, "Themes"),
    ShopTab(ShopCategory.ARROW_SKIN, "Arrow Skins"),
    ShopTab(ShopCategory.TRAIL_FX, "Trail FX"),
    ShopTab(ShopCategory.BOARD_BACKGROUND, "Boards"),
    ShopTab(ShopCategory.HINT_PACK, "Hint Packs"),
    ShopTab(ShopCategory.COIN_PACK, "Coin Packs"),
    ShopTab(ShopCategory.SEASONAL, "Seasonal"),
    ShopTab(ShopCategory.PREMIUM_BUNDLE, "Premium"),
    ShopTab(ShopCategory.LIMITED_EDITION, "Limited"),
)

/**
 * Shop root screen. Horizontal category strip + LazyVerticalGrid of items in
 * the selected category. Top bar shows a coin counter so the player can see
 * their balance at all times.
 *
 * @param onNavigateToItem Called when the user taps a card. Receives the
 *   item id; the NavHost navigates to the detail page.
 * @param onBack Called when the user taps the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: ShopViewModel = hiltViewModel(),
    onNavigateToItem: (String) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedCategory by rememberSaveable { mutableStateOf(ShopCategory.THEME) }

    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is ShopNavEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                ShopNavEvent.Purchased, ShopNavEvent.Equipped -> {
                    // The grid auto-refreshes via the reactive uiState.
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Shop",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    ArrowMazeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
                actions = {
                    val coins = (uiState as? ShopUiState.Success)?.coins ?: 0
                    Row(
                        modifier = Modifier.padding(end = SpacingTokens.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoinCounter(count = coins, size = CoinCounterSize.Medium)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            when (val state = uiState) {
                ShopUiState.Loading -> LoadingState(message = "Loading shop…")
                is ShopUiState.Error -> ErrorState(message = state.message, onRetry = onBack)
                is ShopUiState.Success -> ShopContent(
                    state = state,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    onNavigateToItem = onNavigateToItem,
                    onPurchase = viewModel::purchase,
                    onEquip = viewModel::equip,
                )
            }
        }
    }
}

@Composable
private fun ShopContent(
    state: ShopUiState.Success,
    selectedCategory: ShopCategory,
    onSelectCategory: (ShopCategory) -> Unit,
    onNavigateToItem: (String) -> Unit,
    onPurchase: (String) -> Unit,
    onEquip: (String) -> Unit,
) {
    val items: List<ShopItem> = state.categories[selectedCategory].orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        // Category tabs (horizontal scroll)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            items(shopTabs, key = { it.category.name }) { tab ->
                CategoryTab(
                    label = tab.label,
                    selected = tab.category == selectedCategory,
                    onClick = { onSelectCategory(tab.category) },
                )
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                icon = ArrowMazeIcons.Target,
                title = "Nothing here yet",
                subtitle = "This category is empty.",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SpacingTokens.lg),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
        ) {
            items(items, key = { it.id }) { item ->
                val isOwned = item.id in state.owned
                val equippedId = state.equippedByCategory[item.category.name]
                val isEquipped = equippedId == item.id
                val canAfford = state.coins >= item.price
                val isCosmetic = isCosmeticCategory(item.category)
                val action = resolveItemAction(
                    isOwned = isOwned,
                    isEquipped = isEquipped,
                    pending = state.pendingItemId == item.id,
                    isCosmetic = isCosmetic,
                )
                ShopItemCard(
                    item = item,
                    isOwned = isOwned,
                    isEquipped = isEquipped,
                    canAfford = canAfford,
                    action = action,
                    pending = state.pendingItemId == item.id,
                    onClickCard = { onNavigateToItem(item.id) },
                    onAction = {
                        when (action) {
                            ItemAction.BUY_COINS, ItemAction.BUY_MONEY -> onPurchase(item.id)
                            ItemAction.EQUIP -> onEquip(item.id)
                            else -> { /* no-op */ }
                        }
                    },
                )
            }
        }
    }
}
