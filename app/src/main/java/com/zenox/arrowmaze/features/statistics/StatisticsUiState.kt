package com.zenox.arrowmaze.features.statistics

import com.zenox.arrowmaze.core.domain.model.GameStats
import com.zenox.arrowmaze.core.domain.model.Profile

/**
 * UI state for the Statistics screen.
 *
 * - [Loading] — initial combine of profile + stats.
 * - [Success] — both streams have resolved; render summary cards + charts.
 * - [Error]  — one of the streams threw.
 */
sealed interface StatisticsUiState {

    data object Loading : StatisticsUiState

    data class Success(
        val stats: GameStats,
        val profile: Profile,
        val chartData: ChartData,
    ) : StatisticsUiState

    data class Error(val message: String) : StatisticsUiState
}

/**
 * Pre-computed chart payloads. Derived from [GameStats] so the composables
 * never have to re-bucket data on every recomposition.
 */
data class ChartData(
    val winLoss: WinLossData,
    val solveTimeByLevel: List<SolveTimePoint>,
    val levelDistribution: List<LevelBucket>,
    val recentTrend: List<TrendPoint>,
)

/** Donut payload — wins vs losses counts. */
data class WinLossData(val wins: Int, val losses: Int) {
    val total: Int get() = wins + losses
    val winFraction: Float get() = if (total == 0) 0f else wins.toFloat() / total
}

/** Single (level, solveTimeMs) point on the solve-time line chart. */
data class SolveTimePoint(val level: Int, val solveTimeMs: Long)

/** Single bucket on the level-distribution bar chart. */
data class LevelBucket(
    val rangeLabel: String,
    val minLevel: Int,
    val maxLevel: Int,
    val count: Int,
)

/** Single point on the recent-trend area chart. */
data class TrendPoint(
    val label: String,
    val valueMs: Long,
)
