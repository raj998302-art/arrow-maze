package com.zenox.arrowmaze.features.shop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.ElevationTokens
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Currency
import com.zenox.arrowmaze.core.domain.model.ShopItem

/** Visual affordance for the action button on a [ShopItemCard]. */
enum class ItemAction {
    OWNED,        // item is owned but not equipped
    EQUIPPED,     // item is owned and currently equipped
    EQUIP,        // item is owned, can be equipped
    BUY_COINS,    // item priced in coins; player may or may not afford it
    BUY_MONEY,    // item priced in real money
    PROCESSING,   // a purchase / equip is in flight
}

/**
 * Shop-grid card. Shows a 1:1 preview, title, rarity badge, price row, and
 * a contextual action button (Owned / Equip / Buy).
 *
 * @param item The catalogue item.
 * @param isOwned Whether the player owns the item.
 * @param isEquipped Whether the item is currently equipped in its category.
 * @param canAfford Whether the player has enough coins to buy (COINS items).
 * @param action The action surfaced on the button.
 * @param pending True while a purchase/equip is in flight.
 * @param onClickCard Called when the user taps the card (opens the detail page).
 * @param onAction Called when the user taps the action button.
 */
@Composable
fun ShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    action: ItemAction,
    pending: Boolean,
    onClickCard: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClickCard),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        tonalElevation = ElevationTokens.Level1,
        shadowElevation = ElevationTokens.Level2,
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.md),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                ItemPreview(item = item, modifier = Modifier.fillMaxWidth())
                // Equipped checkmark
                if (isEquipped) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(color = cs.primary, shape = RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✓",
                            color = cs.onPrimary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(SpacingTokens.xs))
                RarityBadge(rarity = item.rarity)
            }

            // Price row
            PriceRow(item = item, canAfford = canAfford)

            // Action button
            ActionButton(
                item = item,
                action = action,
                pending = pending,
                canAfford = canAfford,
                onClick = onAction,
            )
        }
    }
}

@Composable
private fun PriceRow(item: ShopItem, canAfford: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (item.priceCurrency) {
            Currency.COINS -> {
                if (item.price == 0) {
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.tertiary,
                    )
                } else {
                    Icon(
                        imageVector = ArrowMazeIcons.Coin,
                        contentDescription = null,
                        tint = if (canAfford) cs.tertiary else cs.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.price.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (canAfford) cs.onSurface else cs.error,
                    )
                }
            }
            Currency.REAL_MONEY -> {
                val dollars = item.price / 100.0
                Text(
                    text = "$%.2f".format(dollars),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = cs.tertiary,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    item: ShopItem,
    action: ItemAction,
    pending: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    when (action) {
        ItemAction.PROCESSING -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        ItemAction.OWNED -> ArrowMazeButton(
            text = "Owned",
            onClick = onClick,
            style = ButtonStyle.Outline,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
        ItemAction.EQUIPPED -> ArrowMazeButton(
            text = "Equipped",
            onClick = onClick,
            style = ButtonStyle.Tonal,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = ArrowMazeIcons.Target,
        )
        ItemAction.EQUIP -> ArrowMazeButton(
            text = "Equip",
            onClick = onClick,
            style = ButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        ItemAction.BUY_COINS -> ArrowMazeButton(
            text = "Buy",
            onClick = onClick,
            style = ButtonStyle.Primary,
            enabled = canAfford,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = ArrowMazeIcons.Coin,
        )
        ItemAction.BUY_MONEY -> ArrowMazeButton(
            text = "Buy",
            onClick = onClick,
            style = ButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Resolves the [ItemAction] from ownership / equipped / affordability flags. */
fun resolveItemAction(
    isOwned: Boolean,
    isEquipped: Boolean,
    pending: Boolean,
    isCosmetic: Boolean,
): ItemAction = when {
    pending -> ItemAction.PROCESSING
    isEquipped -> ItemAction.EQUIPPED
    isOwned && isCosmetic -> ItemAction.EQUIP
    isOwned -> ItemAction.OWNED
    else -> ItemAction.BUY_COINS
}

/** Helper exported so callers don't need to know the cosmetic-category list. */
fun isCosmeticCategory(category: com.zenox.arrowmaze.core.domain.model.ShopCategory): Boolean =
    category in setOf(
        com.zenox.arrowmaze.core.domain.model.ShopCategory.THEME,
        com.zenox.arrowmaze.core.domain.model.ShopCategory.ARROW_SKIN,
        com.zenox.arrowmaze.core.domain.model.ShopCategory.TRAIL_FX,
        com.zenox.arrowmaze.core.domain.model.ShopCategory.BOARD_BACKGROUND,
        com.zenox.arrowmaze.core.domain.model.ShopCategory.SEASONAL,
        com.zenox.arrowmaze.core.domain.model.ShopCategory.PREMIUM_BUNDLE,
        com.zenox.arrowmaze.core.domain.model.ShopCategory.LIMITED_EDITION,
    )
