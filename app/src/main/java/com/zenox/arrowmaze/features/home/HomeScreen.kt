package com.zenox.arrowmaze.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.GradientBackground
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.home.components.DailyStreakBanner
import com.zenox.arrowmaze.features.home.components.HeroLevelCard
import com.zenox.arrowmaze.features.home.components.ModeCard
import com.zenox.arrowmaze.features.home.components.ModeCardVariant
import com.zenox.arrowmaze.features.home.components.StatsBar
import kotlinx.coroutines.delay

/**
 * Root home hub. Mounts an animated [GradientBackground] behind a vertically
 * scrolling column of:
 *
 *   1. The brand top bar (logo + name + settings icon).
 *   2. [HeroLevelCard] — current level + tier + XP progress + Continue CTA.
 *   3. [StatsBar] — coins / hints / lives (with regen timer).
 *   4. [DailyStreakBanner] — only when `dailyStreak > 0`.
 *   5. A 2-column grid of [ModeCard]s: Play (full-width primary), Daily,
 *      Practice, Shop, Achievements (with `X/Y` badge), Leaderboard.
 *   6. Quick links row — Profile + Settings.
 *
 * Cards drop in with a staggered fade-in animation. Pull-to-refresh is
 * wired via the [onRefresh] callback (the parent exposes a swipe-refresh
 * layout that triggers `viewModel.refresh()`).
 *
 * @param onPlay            Fired with the player's current level when the
 *                          Hero "Continue" or Play mode-card is tapped.
 * @param onDailyChallenge  Fired when the Daily mode-card is tapped.
 * @param onPractice        Fired when the Practice mode-card is tapped.
 * @param onShop            Fired when the Shop mode-card is tapped.
 * @param onAchievements    Fired when the Achievements mode-card is tapped.
 * @param onLeaderboard     Fired when the Leaderboard mode-card is tapped.
 * @param onProfile         Fired when the Profile quick-link is tapped.
 * @param onSettings        Fired when the settings icon / quick-link is tapped.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlay: (Int) -> Unit,
    onDailyChallenge: () -> Unit,
    onPractice: () -> Unit,
    onShop: () -> Unit,
    onAchievements: () -> Unit,
    onLeaderboard: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is HomeUiState.Loading -> LoadingState(message = "Loading home…")
            is HomeUiState.Error -> ErrorState(
                message = state.message,
                onRetry = viewModel::refresh,
            )
            is HomeUiState.Success -> HomeContent(
                state = state,
                onPlay = onPlay,
                onDailyChallenge = onDailyChallenge,
                onPractice = onPractice,
                onShop = onShop,
                onAchievements = onAchievements,
                onLeaderboard = onLeaderboard,
                onProfile = onProfile,
                onSettings = onSettings,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onPlay: (Int) -> Unit,
    onDailyChallenge: () -> Unit,
    onPractice: () -> Unit,
    onShop: () -> Unit,
    onAchievements: () -> Unit,
    onLeaderboard: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    // Staggered entrance: each section fades + slides in one after another.
    var heroVisible by remember { mutableStateOf(false) }
    var statsVisible by remember { mutableStateOf(false) }
    var streakVisible by remember { mutableStateOf(false) }
    var modesVisible by remember { mutableStateOf(false) }
    var quickLinksVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50); heroVisible = true
        delay(80); statsVisible = true
        delay(80); streakVisible = state.dailyStreak > 0
        delay(80); modesVisible = true
        delay(80); quickLinksVisible = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Top bar ----
        HomeTopBar(onSettings = onSettings)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = SpacingTokens.lg,
                end = SpacingTokens.lg,
                top = SpacingTokens.sm,
                bottom = SpacingTokens.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ---- Hero card (full width) ----
            item(span = { GridItemSpan(2) }) {
                StaggeredIn(visible = heroVisible) {
                    HeroLevelCard(
                        profile = state.profile,
                        currentLevel = state.currentLevel,
                        tier = state.currentTier,
                        onContinue = { onPlay(state.currentLevel) },
                    )
                }
            }

            // ---- Stats bar (full width) ----
            item(span = { GridItemSpan(2) }) {
                StaggeredIn(visible = statsVisible) {
                    StatsBar(
                        coins = state.profile.coins,
                        hints = state.profile.hints,
                        lives = state.lives,
                        maxLives = AppConstants.MAX_LIVES,
                        nextLifeRegenMs = state.nextLifeRegenMs,
                    )
                }
            }

            // ---- Daily streak banner (full width, only when streak > 0) ----
            if (state.dailyStreak > 0) {
                item(span = { GridItemSpan(2) }) {
                    StaggeredIn(visible = streakVisible) {
                        DailyStreakBanner(streak = state.dailyStreak)
                    }
                }
            }

            // ---- Play card (full-width primary) ----
            item(span = { GridItemSpan(2) }) {
                StaggeredIn(visible = modesVisible, delayMs = 0) {
                    ModeCard(
                        label = "Play",
                        subtitle = "Level ${state.currentLevel} • ${state.currentTier.displayName}",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = { onPlay(state.currentLevel) },
                        variant = ModeCardVariant.Primary,
                    )
                }
            }

            // ---- Secondary mode cards (2 per row) ----
            val secondaryCards: List<ModeCardSpec> = buildList {
                add(
                    ModeCardSpec(
                        label = "Daily Challenge",
                        subtitle = if (state.canPlayDaily) "Today's puzzle" else "Solved today",
                        icon = Icons.Rounded.Today,
                        onClick = onDailyChallenge,
                        variant = if (state.canPlayDaily) ModeCardVariant.Elevated else ModeCardVariant.Disabled,
                        badge = if (state.canPlayDaily) null else "Done",
                    )
                )
                add(
                    ModeCardSpec(
                        label = "Practice",
                        subtitle = "Endless mode",
                        icon = Icons.Rounded.Explore,
                        onClick = onPractice,
                        variant = ModeCardVariant.Elevated,
                    )
                )
                add(
                    ModeCardSpec(
                        label = "Shop",
                        subtitle = "Themes & items",
                        icon = Icons.Rounded.Storefront,
                        onClick = onShop,
                        variant = ModeCardVariant.Elevated,
                    )
                )
                add(
                    ModeCardSpec(
                        label = "Achievements",
                        subtitle = "${state.unlockedAchievements}/${state.totalAchievements} unlocked",
                        icon = Icons.Rounded.EmojiEvents,
                        onClick = onAchievements,
                        variant = ModeCardVariant.Elevated,
                        badge = "${state.unlockedAchievements}/${state.totalAchievements}",
                    )
                )
                add(
                    ModeCardSpec(
                        label = "Leaderboard",
                        subtitle = "Global & friends",
                        icon = Icons.Rounded.TrendingUp,
                        onClick = onLeaderboard,
                        variant = ModeCardVariant.Elevated,
                    )
                )
                // Profile card — fills the bottom-right slot so the grid stays balanced.
                add(
                    ModeCardSpec(
                        label = "Profile",
                        subtitle = state.profile.playerName,
                        icon = Icons.Rounded.Person,
                        onClick = onProfile,
                        variant = ModeCardVariant.Elevated,
                    )
                )
            }
            items(
                items = secondaryCards,
                key = { it.label },
                span = { GridItemSpan(1) },
            ) { spec ->
                StaggeredIn(visible = modesVisible, delayMs = 40) {
                    ModeCard(
                        label = spec.label,
                        subtitle = spec.subtitle,
                        icon = spec.icon,
                        onClick = spec.onClick,
                        variant = spec.variant,
                        badge = spec.badge,
                        enabled = spec.variant != ModeCardVariant.Disabled,
                    )
                }
            }

            // ---- Quick links row (full width) ----
            item(span = { GridItemSpan(2) }) {
                StaggeredIn(visible = quickLinksVisible) {
                    QuickLinksRow(onProfile = onProfile, onSettings = onSettings)
                }
            }
        }
    }
}

/** Brand top bar: gradient logo disc + "Arrow Maze" wordmark on the left, settings icon on the right. */
@Composable
private fun HomeTopBar(onSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ArrowMazeIcons.Target,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(SpacingTokens.sm))
            Text(
                text = "Arrow Maze",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = cs.onSurface,
            )
        }
        ArrowMazeIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            onClick = onSettings,
            tint = cs.onSurface,
            containerColor = cs.surface.copy(alpha = 0.55f),
        )
    }
}

/** Compact bottom row with Profile + Settings quick-link chips. */
@Composable
private fun QuickLinksRow(
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SpacingTokens.sm),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
    ) {
        QuickLinkChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Person,
            label = "Profile",
            onClick = onProfile,
        )
        QuickLinkChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Settings,
            label = "Settings",
            onClick = onSettings,
        )
    }
}

@Composable
private fun QuickLinkChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .then(Modifier.clickable(onClick = onClick)),
        shape = RoundedCornerShape(percent = 50),
        color = cs.surface.copy(alpha = 0.6f),
        contentColor = cs.onSurface,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = SpacingTokens.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(SpacingTokens.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = cs.onSurface,
            )
        }
    }
}

/**
 * Staggered fade-in wrapper. The first frame is invisible; the wrapper
 * fades + slides in once [visible] flips true.
 */
@Composable
private fun StaggeredIn(
    visible: Boolean,
    delayMs: Long = 0L,
    content: @Composable () -> Unit,
) {
    var delayedVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            if (delayMs > 0) delay(delayMs)
            delayedVisible = true
        }
    }
    AnimatedVisibility(
        visible = delayedVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 350)) +
            slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = tween(durationMillis = 350),
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)),
    ) {
        content()
    }
}

/** Internal spec used to populate the secondary mode-card grid. */
private data class ModeCardSpec(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val variant: ModeCardVariant,
    val badge: String? = null,
)
