package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Snapshot of the parameters used by [com.zenox.arrowmaze.core.domain.engine.PuzzleGenerator]
 * to build a single level. Produced by [LevelProgression.configFor].
 *
 * @property level              1-based level number, the canonical player-facing key.
 * @property tier               Difficulty tier badge.
 * @property boardSize          Edge length (N) of the N×N board.
 * @property arrowCount         Target number of [Cell.ArrowCell]s on the generated board.
 * @property arrowDensity       Fraction (0..1) of cells that should hold arrows (≈ arrowCount / (boardSize² - 2)).
 * @property pathComplexity     0..1 — how tortuous the carved solution path should be.
 * @property thinkingDifficulty 0..1 — abstract rating used by XP / star rewards and analytics.
 */
@Serializable
data class LevelConfig(
    @SerialName("level")              val level: Int,
    @SerialName("tier")               val tier: DifficultyTier,
    @SerialName("boardSize")          val boardSize: Int,
    @SerialName("arrowCount")         val arrowCount: Int,
    @SerialName("arrowDensity")       val arrowDensity: Float,
    @SerialName("pathComplexity")     val pathComplexity: Float,
    @SerialName("thinkingDifficulty") val thinkingDifficulty: Float
) {

    /** Suggested move cap. The player gets enough budget to rotate every arrow at least once. */
    val suggestedMaxMoves: Int get() = boardSize * boardSize

    override fun toString(): String =
        "LevelConfig(level=$level, tier=$tier, board=${boardSize}x$boardSize, " +
            "arrows=$arrowCount, density=$arrowDensity)"
}
