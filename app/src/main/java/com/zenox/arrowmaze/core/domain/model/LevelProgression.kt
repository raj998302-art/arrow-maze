package com.zenox.arrowmaze.core.domain.model

/**
 * Pure rule engine that converts a 1-based level number into a fully-populated [LevelConfig].
 *
 * Tier table (matches the design spec):
 *
 * | Level range | Tier    | Board | Density   |
 * |-------------|---------|-------|-----------|
 * | 1–20        | EASY    | 4×4   | 0.40–0.55 |
 * | 21–50       | NORMAL  | 5×5   | 0.50–0.60 |
 * | 51–100      | HARD    | 6×6   | 0.55–0.65 |
 * | 101–200     | EXPERT  | 7×7   | 0.60–0.70 |
 * | 201–400     | MASTER  | 8×8   | 0.65–0.72 |
 * | 401+        | LEGEND  | 9×9   | 0.70–0.78 |
 *
 * Inside each tier the density grows linearly from its min to its max across the tier's level
 * span (the LEGEND tier spans 200 virtual levels then clamps at its max so progression stays
 * bounded and stable for very high level numbers).
 *
 * `pathComplexity` and `thinkingDifficulty` grow from 0.30 → 0.95 in lock-step with density,
 * so harder levels reward more XP / stars.
 */
object LevelProgression {

    /** Defines a single tier's level span and parameter growth. */
    private data class TierBand(
        val tier: DifficultyTier,
        val levelStart: Int,
        val levelEnd: Int,             // inclusive; LEGEND uses Int.MAX_VALUE
        val spanForInterpolation: Int  // virtual span used when interpolating density/complexity
    )

    private val bands: List<TierBand> = listOf(
        TierBand(DifficultyTier.EASY,   1,   20,  20),
        TierBand(DifficultyTier.NORMAL, 21,  50,  30),
        TierBand(DifficultyTier.HARD,   51,  100, 50),
        TierBand(DifficultyTier.EXPERT, 101, 200, 100),
        TierBand(DifficultyTier.MASTER, 201, 400, 200),
        TierBand(DifficultyTier.LEGEND, 401, Int.MAX_VALUE, 200)
    )

    /** Resolves which tier band a given level belongs to. */
    fun tierFor(level: Int): DifficultyTier {
        require(level >= 1) { "Level must be >= 1, was $level" }
        return bands.first { level in it.levelStart..it.levelEnd }.tier
    }

    /**
     * Linear interpolation helper that clamps to [0,1].
     * [progressInTier] is the fractional position (0..1) of the level inside its tier.
     */
    private fun lerpClamped(min: Float, max: Float, progressInTier: Float): Float {
        val raw = min + (max - min) * progressInTier.coerceIn(0f, 1f)
        return raw.coerceIn(min.coerceAtMost(max), min.coerceAtLeast(max))
    }

    /**
     * Builds the [LevelConfig] for [level]. Deterministic and side-effect free.
     */
    fun configFor(level: Int): LevelConfig {
        require(level >= 1) { "Level must be >= 1, was $level" }
        val band = bands.first { level in it.levelStart..it.levelEnd }
        val tier = band.tier

        // Fractional position inside the tier (0..1). For LEGEND (infinite), the position
        // saturates at 1.0 once the virtual span is exceeded.
        val offset = (level - band.levelStart).toFloat()
        val span = band.spanForInterpolation.toFloat()
        val progress = (offset / span).coerceIn(0f, 1f)

        val densityMin = tier.arrowDensityRange.start
        val densityMax = tier.arrowDensityRange.endInclusive
        val density = lerpClamped(densityMin, densityMax, progress)

        val boardSize = tier.boardSizeRange.first // single fixed size per tier
        val totalCells = boardSize * boardSize
        val arrowCount = kotlin.math.round(totalCells * density).toInt()
            .coerceAtLeast(2)              // need at least 2 arrows for a path
            .coerceAtMost(totalCells - 2)  // never crowd out start/goal

        val pathComplexity = lerpClamped(0.30f, 0.95f, progress)
        val thinkingDifficulty = lerpClamped(0.30f, 0.95f, progress)

        return LevelConfig(
            level = level,
            tier = tier,
            boardSize = boardSize,
            arrowCount = arrowCount,
            arrowDensity = density,
            pathComplexity = pathComplexity,
            thinkingDifficulty = thinkingDifficulty
        )
    }

    /** Convenience: how many levels live in the given tier. */
    fun tierSize(tier: DifficultyTier): Int {
        val band = bands.first { it.tier == tier }
        return if (band.levelEnd == Int.MAX_VALUE) Int.MAX_VALUE
        else band.levelEnd - band.levelStart + 1
    }

    /** Convenience: first level number that belongs to the given tier. */
    fun firstLevelOf(tier: DifficultyTier): Int =
        bands.first { it.tier == tier }.levelStart
}
