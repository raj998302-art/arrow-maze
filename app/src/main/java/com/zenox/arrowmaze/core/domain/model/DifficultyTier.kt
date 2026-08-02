package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Six difficulty tiers. The board generator ([com.zenox.arrowmaze.core.domain.engine.PuzzleGenerator])
 * and the level progression ([LevelProgression]) both read the tier metadata to know what board
 * size and arrow density to aim for.
 *
 * @property displayName         Human label shown in the UI badge.
 * @property boardSizeRange      Inclusive board-size range the tier may use.
 * @property arrowDensityRange   Fraction (0..1) of non-start/goal cells that should hold arrows.
 * @property colorHex            Hex colour (e.g. `"#4CAF50"`) used by the UI badge.
 * @property sortOrder           Lower = easier. Stable across versions for sorting.
 */
@Serializable
enum class DifficultyTier(
    val displayName: String,
    val boardSizeRange: IntRange,
    val arrowDensityRange: ClosedFloatingPointRange<Float>,
    val colorHex: String,
    val sortOrder: Int
) {
    @SerialName("EASY")   EASY  ("Easy",   4..4, 0.40f..0.55f, "#4CAF50", 0),
    @SerialName("NORMAL") NORMAL("Normal", 5..5, 0.50f..0.60f, "#2196F3", 1),
    @SerialName("HARD")   HARD  ("Hard",   6..6, 0.55f..0.65f, "#FF9800", 2),
    @SerialName("EXPERT") EXPERT("Expert", 7..7, 0.60f..0.70f, "#F44336", 3),
    @SerialName("MASTER") MASTER("Master", 8..8, 0.65f..0.72f, "#9C27B0", 4),
    @SerialName("LEGEND") LEGEND("Legend", 9..9, 0.70f..0.78f, "#FFD700", 5);

    companion object {
        /** Tier at or below the given sort order; for UI progression arrows. */
        fun fromSortOrder(order: Int): DifficultyTier =
            entries.firstOrNull { it.sortOrder == order } ?: entries.last()
    }
}
