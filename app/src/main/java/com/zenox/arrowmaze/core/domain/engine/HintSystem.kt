package com.zenox.arrowmaze.core.domain.engine

import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.Cell
import com.zenox.arrowmaze.core.domain.model.Position

/**
 * Greedy hint solver. Given a board that is not yet solved, [findHint] returns the [Position]
 * of an arrow cell whose rotation (by 90°, 180° or 270° CW) makes the most progress toward a
 * full solution — ideally solving the puzzle outright.
 *
 * ## Scoring
 *
 * For each arrow cell on the board we try all three non-trivial rotations and pick the one
 * that yields the longest path prefix (see [WinDetector.pathPrefix]). If any rotation solves
 * the board, that cell is returned immediately as the highest-priority hint.
 *
 * If no rotation improves on the current prefix length (i.e. the player is already as far
 * along as any single rotation can take them), `null` is returned — the caller can then
 * surface a "no hint available" message or grant the player a free cell reveal.
 *
 * The algorithm is O(A · R · N²) where A is the arrow count, R = 3 rotations and N is the
 * board size — well under 1 ms for a 9×9 board.
 */
object HintSystem {

    /** Rotations to try (1 = 90° CW, 2 = 180°, 3 = 270°). 0 is the current state. */
    private val ROTATIONS = intArrayOf(1, 2, 3)

    /**
     * Returns the [Position] of the most useful arrow to rotate next, or `null` if the board
     * is already solved or no rotation makes progress.
     */
    fun findHint(board: Board): Position? {
        // Already solved — nothing to hint.
        if (WinDetector.isSolved(board)) return null

        val baseline = WinDetector.pathPrefix(board).size
        var bestPos: Position? = null
        var bestPrefix = baseline

        for ((pos, _) in board.arrowCells()) {
            for (times in ROTATIONS) {
                val candidate = board.withArrowRotatedBy(pos, times)

                // Fast path: this rotation solves the puzzle outright.
                if (WinDetector.isSolved(candidate)) return pos

                val candidatePrefix = WinDetector.pathPrefix(candidate).size
                if (candidatePrefix > bestPrefix) {
                    bestPrefix = candidatePrefix
                    bestPos = pos
                }
            }
        }
        return bestPos
    }

    /**
     * Returns the [Position] AND the exact number of 90° CW rotations that yield the best
     * progress, so the engine can apply the hint deterministically (rather than re-deriving
     * the rotation). Returns `null` when no useful hint exists.
     */
    fun findHintWithRotation(board: Board): Pair<Position, Int>? {
        if (WinDetector.isSolved(board)) return null

        val baseline = WinDetector.pathPrefix(board).size
        var best: Triple<Position, Int, Int>? = null // (pos, rotations, prefixLen)

        for ((pos, _) in board.arrowCells()) {
            for (times in ROTATIONS) {
                val candidate = board.withArrowRotatedBy(pos, times)
                if (WinDetector.isSolved(candidate)) {
                    // Immediate win — return without considering further candidates.
                    return pos to times
                }
                val candidatePrefix = WinDetector.pathPrefix(candidate).size
                if (best == null || candidatePrefix > best.third) {
                    best = Triple(pos, times, candidatePrefix)
                }
            }
        }
        // Only return a hint if it actually improves on the baseline.
        return if (best != null && best.third > baseline) best.first to best.second else null
    }
}
