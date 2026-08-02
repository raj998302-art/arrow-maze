package com.zenox.arrowmaze.features.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.EmptyState
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.LeaderboardEntry
import com.zenox.arrowmaze.core.domain.model.LeaderboardScope
import com.zenox.arrowmaze.features.leaderboard.components.LeaderboardRow
import com.zenox.arrowmaze.features.leaderboard.components.PodiumCard
import com.zenox.arrowmaze.features.leaderboard.components.PodiumVariant

/**
 * Root composable for the Leaderboard screen.
 *
 * Layout:
 *  - Center-aligned top bar with back nav.
 *  - Scrollable tab row (Global / Friends / Weekly / Monthly / All Time).
 *  - Below the tabs: a podium of the top 3 finishers, then a [LazyColumn]
 *    of ranks 4+. The current user's row is highlighted via [LeaderboardRow].
 *  - Pull-to-refresh via Material 3 `PullToRefreshBox`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedScope by viewModel.selectedScope.collectAsStateWithLifecycle()

    val scopes = LeaderboardScope.entries
    val tabLabels = listOf(
        stringResource(R.string.leaderboard_global),
        stringResource(R.string.leaderboard_friends),
        stringResource(R.string.leaderboard_weekly),
        stringResource(R.string.leaderboard_monthly),
        stringResource(R.string.leaderboard_all_time),
    )
    val selectedIndex = scopes.indexOf(selectedScope).coerceAtLeast(0)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    ArrowMazeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_button),
                        onClick = onBack,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                scopes.forEachIndexed { index, scope ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { viewModel.selectScope(scope) },
                        text = { Text(tabLabels[index], maxLines = 1) },
                    )
                }
            }

            PullRefreshContainer(
                isRefreshing = (uiState as? LeaderboardUiState.Success)?.isRefreshing == true,
                onRefresh = viewModel::refresh,
            ) {
                when (val state = uiState) {
                    is LeaderboardUiState.Loading -> LoadingState(message = "Loading leaderboard…")
                    is LeaderboardUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = viewModel::refresh,
                    )
                    is LeaderboardUiState.Success -> LeaderboardContent(
                        state = state,
                        onRefresh = viewModel::refresh,
                    )
                }
            }
        }
    }
}

/**
 * Pull-to-refresh container. Uses Material 3 [PullToRefreshBox] when
 * available; the indeterminate indicator is shown only while [isRefreshing]
 * is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
    }
}

@Composable
private fun LeaderboardContent(
    state: LeaderboardUiState.Success,
    onRefresh: () -> Unit,
) {
    val entries = state.entries
    if (entries.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.EmojiEvents,
            title = "No entries yet",
            subtitle = "Be the first to claim a spot on the leaderboard!",
        )
        return
    }

    val topThree = entries.take(3)
    val rest = entries.drop(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = SpacingTokens.sm,
            vertical = SpacingTokens.md,
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        if (topThree.isNotEmpty()) {
            item(key = "podium") {
                PodiumRow(topThree)
            }
            item(key = "podium_spacer") {
                Spacer(Modifier.height(SpacingTokens.lg))
            }
        }

        if (rest.isEmpty() && state.myRank != null && state.myRank <= 3) {
            // Already on the podium — no extra row to show.
            return@LazyColumn
        }

        // My rank callout if I'm outside the visible top range.
        if (state.myRank != null && state.myRank > 3) {
            val myEntry = entries.firstOrNull { it.isCurrentUser }
            if (myEntry != null && myEntry.rank > 3) {
                item(key = "my_rank_callout") {
                    MyRankCallout(entry = myEntry, totalCount = entries.size)
                    Spacer(Modifier.height(SpacingTokens.sm))
                }
            }
        }

        items(items = rest, key = { it.uid + "-${it.rank}" }) { entry ->
            LeaderboardRow(entry = entry)
        }

        item(key = "footer_spacer") {
            Spacer(Modifier.height(SpacingTokens.xxl))
        }
    }
}

/**
 * Top-3 podium. The cards are arranged as `[Silver, Gold, Bronze]` so the
 * gold winner is visually centred.
 */
@Composable
private fun PodiumRow(topThree: List<LeaderboardEntry>) {
    val gold = topThree.getOrNull(0)
    val silver = topThree.getOrNull(1)
    val bronze = topThree.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.sm),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (silver != null) {
            Box(modifier = Modifier.weight(1f)) {
                PodiumCard(entry = silver, variant = PodiumVariant.Silver)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (gold != null) {
            Box(modifier = Modifier.weight(1f)) {
                PodiumCard(entry = gold, variant = PodiumVariant.Gold)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (bronze != null) {
            Box(modifier = Modifier.weight(1f)) {
                PodiumCard(entry = bronze, variant = PodiumVariant.Bronze)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Compact callout that surfaces the current user's rank when they're below rank 3. */
@Composable
private fun MyRankCallout(entry: LeaderboardEntry, totalCount: Int) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.md),
        color = cs.primaryContainer,
        contentColor = cs.onPrimaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleLarge,
                color = cs.primary,
                fontWeight = FontWeight.Bold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = "${entry.displayName} • ${entry.xp} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "of $totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}
