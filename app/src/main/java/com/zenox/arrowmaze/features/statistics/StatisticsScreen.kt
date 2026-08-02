package com.zenox.arrowmaze.features.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeCard
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.statistics.components.LevelDistributionChart
import com.zenox.arrowmaze.features.statistics.components.SolveTimeChart
import com.zenox.arrowmaze.features.statistics.components.SummaryCard
import com.zenox.arrowmaze.features.statistics.components.TrendChart
import com.zenox.arrowmaze.features.statistics.components.WinLossChart
import java.util.concurrent.TimeUnit

/**
 * Root composable for the Statistics screen.
 *
 * Layout:
 *  - Center-aligned top bar with back nav.
 *  - Scrollable column: 2x3 summary card grid, win/loss donut, solve-time
 *    line chart, level-distribution bar chart, recent-trend area chart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Statistics",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is StatisticsUiState.Loading -> LoadingState(message = "Loading statistics…")
                is StatisticsUiState.Error -> ErrorState(message = state.message)
                is StatisticsUiState.Success -> StatisticsContent(state = state)
            }
        }
    }
}

@Composable
private fun StatisticsContent(state: StatisticsUiState.Success) {
    val stats = state.stats
    val profile = state.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
    ) {
        // Summary cards — 2 columns × 3 rows.
        val summaryItems: List<SummaryItem> = listOf(
            SummaryItem(
                icon = Icons.Rounded.SportsEsports,
                value = stats.totalGames.toString(),
                label = "Total games",
            ),
            SummaryItem(
                icon = Icons.Rounded.EmojiEvents,
                value = "${(stats.winRate * 100).toInt()}%",
                label = "Win rate",
            ),
            SummaryItem(
                icon = Icons.Rounded.Speed,
                value = formatMs(stats.fastestSolveMs),
                label = "Fastest solve",
            ),
            SummaryItem(
                icon = Icons.Rounded.LocalFireDepartment,
                value = stats.bestStreak.toString(),
                label = "Best streak",
            ),
            SummaryItem(
                icon = Icons.Rounded.Timer,
                value = formatMs(stats.averageSolveTimeMs),
                label = "Avg solve time",
            ),
            SummaryItem(
                icon = Icons.Rounded.Insights,
                value = formatDurationMs(stats.totalTimeMs),
                label = "Total time played",
            ),
        )
        summaryItems.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        SummaryCard(
                            icon = item.icon,
                            value = item.value,
                            label = item.label,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Profile meta
        ArrowMazeCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
            ) {
                Text(
                    text = "Player: ${profile.displayName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Level ${profile.level}  •  ${profile.xp} XP  •  Highest level ${profile.highestLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Current streak: ${stats.currentStreak}  •  Total moves: ${stats.totalMoves}  •  Hints used: ${stats.totalHintsUsed}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Charts
        ArrowMazeCard { WinLossChart(data = state.chartData.winLoss) }
        ArrowMazeCard { SolveTimeChart(points = state.chartData.solveTimeByLevel) }
        ArrowMazeCard { LevelDistributionChart(buckets = state.chartData.levelDistribution) }
        ArrowMazeCard { TrendChart(points = state.chartData.recentTrend) }

        Spacer(Modifier.height(SpacingTokens.xxl))
    }
}

private data class SummaryItem(
    val icon: ImageVector,
    val value: String,
    val label: String,
)

/** Formats a millisecond duration as `M:SS` (or `0:00` when zero). */
private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "—"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Formats a long millisecond duration as `Hh Mm` / `Mm Ss` / `Ss`. */
private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "—"
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
