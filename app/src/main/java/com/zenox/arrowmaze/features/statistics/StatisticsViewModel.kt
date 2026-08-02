package com.zenox.arrowmaze.features.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.data.repository.StatsRepository
import com.zenox.arrowmaze.core.domain.model.GameStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the Statistics screen.
 *
 * Subscribes to the current user's profile + stats flows and derives a
 * [ChartData] payload from [GameStats] so the composables don't have to
 * re-bucket on every recomposition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.currentUidFlow
                .flatMapLatest { uid ->
                    if (uid == null) {
                        kotlinx.coroutines.flow.flowOf(StatisticsUiState.Error("Not signed in"))
                    } else {
                        combine(
                            profileRepository.observeProfile(uid),
                            statsRepository.observeStats(uid),
                        ) { profile, stats ->
                            if (profile == null) {
                                StatisticsUiState.Loading
                            } else {
                                StatisticsUiState.Success(
                                    stats = stats,
                                    profile = profile,
                                    chartData = stats.toChartData(),
                                )
                            }
                        }
                    }
                }
                .catch { t ->
                    Timber.e(t, "Statistics stream failed")
                    emit(StatisticsUiState.Error(t.message ?: "Failed to load statistics"))
                }
                .collect { state -> _uiState.value = state }
        }
    }

    /**
     * Derives a [ChartData] payload from a [GameStats] aggregate.
     *
     * - Win/loss donut: straight from `totalWins` / `computedLosses`.
     * - Solve-time-by-level line chart: `solveTimesByLevel` sorted by level.
     * - Level-distribution bar chart: solved levels bucketed into ranges of 25.
     * - Recent-trend area chart: last 30 solve times (or all if fewer) — used
     *   as a proxy for "last 30 games" since the data layer only stores per
     *   level, not per game. Phase 10 can swap this for a real per-game
     *   history once the Firestore `games` collection exists.
     */
    private fun GameStats.toChartData(): ChartData {
        val wins = totalWins.coerceAtLeast(0)
        val losses = computedLosses.coerceAtLeast(0)

        val solveTimePoints = solveTimesByLevel
            .entries
            .sortedBy { it.key }
            .map { SolveTimePoint(level = it.key, solveTimeMs = it.value) }

        val levelBuckets = buildLevelBuckets(solveTimesByLevel.keys)

        val trend = solveTimePoints
            .takeLast(30)
            .mapIndexed { idx, point ->
                TrendPoint(label = "L${point.level}", valueMs = point.solveTimeMs)
            }

        return ChartData(
            winLoss = WinLossData(wins = wins, losses = losses),
            solveTimeByLevel = solveTimePoints,
            levelDistribution = levelBuckets,
            recentTrend = trend,
        )
    }

    /** Buckets the solved-level numbers into ranges of 25 for the bar chart. */
    private fun buildLevelBuckets(levels: Set<Int>): List<LevelBucket> {
        if (levels.isEmpty()) return emptyList()
        val minLevel = (levels.min() / 25) * 25 + 1
        val maxLevel = ((levels.max() / 25) + 1) * 25
        val buckets = mutableListOf<LevelBucket>()
        var current = minLevel
        while (current <= maxLevel) {
            val upper = current + 24
            val count = levels.count { it in current..upper }
            buckets += LevelBucket(
                rangeLabel = "$current–$upper",
                minLevel = current,
                maxLevel = upper,
                count = count,
            )
            current += 25
        }
        return buckets
    }
}
