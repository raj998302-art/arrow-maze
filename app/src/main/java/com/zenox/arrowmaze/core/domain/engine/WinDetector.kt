package com.zenox.arrowmaze.core.domain.engine

import com.zenox.arrowmaze.core.domain.model.ArrowDirection
import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.Cell
import com.zenox.arrowmaze.core.domain.model.Position

/**
 * Decides whether a [Board] is currently solved and, if so, returns the exact Start → Goal
 * path that solves it.
 *
 * ## Path traversal model
 *
 * The path begins at [Board.start] and ends at [Board.goal]. Each cell visited contributes
 * a direction:
 *
 *  - **StartCell**  — the start position is on a board edge; the path enters the board by
 *    stepping *into* the grid (away from that edge). The implicit entry direction is:
 *      * top edge    (row == 0)            → DOWN
 *      * bottom edge (row == size-1)       → UP
 *      * left edge   (col == 0)            → RIGHT
 *      * right edge  (col == size-1)       → LEFT
 *    (Top/bottom take priority over left/right when the start is in a corner.)
 *
 *  - **ArrowCell**  — the player's rotatable cell; the arrow's current direction is followed.
 *
 *  - **GoalCell**   — terminal; reaching it ends the path successfully.
 *
 *  - **EmptyCell**  — decorative gap; landing on it (or stepping out of bounds, or revisiting
 *    a cell) breaks the chain and the board is considered unsolved.
 *
 * The traversal is deterministic, side-effect free, and capped at `size * size` steps so a
 * pathological input can never spin the engine.
 */
object WinDetector {

    /**
     * Returns `true` if the path from [Board.start] reaches [Board.goal] following the rules
     * described above. Implemented in terms of [solvePath] for clarity; the path-prefix
     * allocation is negligible for boards up to 9×9.
     */
    fun isSolved(board: Board): Boolean = solvePath(board) != null

    /**
     * Returns the ordered list of positions from [Board.start] to [Board.goal] if the board
     * is solved, or `null` if it isn't. The list includes both endpoints.
     */
    fun solvePath(board: Board): List<Position>? {
        val prefix = pathPrefix(board)
        // The prefix is a full solution iff its last cell is the goal.
        return if (prefix.isNotEmpty() && prefix.last() == board.goal) prefix else null
    }

    /**
     * Returns the longest valid prefix of the path traced from [Board.start]. The prefix
     * stops at the first "break":
     *  - stepping out of bounds,
     *  - landing on an [Cell.EmptyCell] (the EmptyCell itself is NOT included),
     *  - revisiting a cell (cycle),
     *  - or reaching the [Cell.GoalCell] (goal IS included, marking a solved board).
     *
     * Used by [HintSystem] (to score hint candidates) and [GameEngine] (to expose the live
     * partial path to the UI). Always returns a non-null list; may be empty if even the
     * start cell can't be stepped off.
     */
    fun pathPrefix(board: Board): List<Position> {
        val path = mutableListOf<Position>()
        val visited = HashSet<Position>()

        var current = board.start
        // Hard cap to defend against pathological inputs: a board of size N has N² cells,
        // so the longest possible non-cyclic path is N² cells long.
        val maxSteps = board.size * board.size + 1

        repeat(maxSteps) {
            // Out-of-bounds sanity (start should always be in-bounds, but be defensive).
            if (!current.isWithin(board.size)) return path
            // Cycle detection.
            if (!visited.add(current)) return path

            val cell = board.cellAt(current)

            when (cell) {
                is Cell.EmptyCell -> {
                    // Decorative gap — do NOT include in prefix; chain broken.
                    return path
                }
                is Cell.GoalCell -> {
                    // Reached the goal — terminal, include it and return.
                    path.add(current)
                    return path
                }
                is Cell.StartCell -> {
                    path.add(current)
                    val dir = entryDirectionFor(current, board.size) ?: return path
                    val next = step(current, dir, board.size) ?: return path
                    current = next
                }
                is Cell.ArrowCell -> {
                    path.add(current)
                    val next = step(current, cell.direction, board.size) ?: return path
                    current = next
                }
            }
        }
        // Exceeded the step cap — return whatever prefix we collected.
        return path
    }

    /**
     * Returns the implicit entry direction for a [start] position on the edge of a board of
     * side [size], or `null` if [start] is in the interior (which would be a generation bug).
     */
    internal fun entryDirectionFor(start: Position, size: Int): ArrowDirection? = when {
        start.row == 0          -> ArrowDirection.DOWN
        start.row == size - 1   -> ArrowDirection.UP
        start.col == 0          -> ArrowDirection.RIGHT
        start.col == size - 1   -> ArrowDirection.LEFT
        else                    -> null
    }

    /**
     * Steps from [pos] one cell in [dir], returning the new position or `null` if it would
     * leave the board.
     */
    private fun step(pos: Position, dir: ArrowDirection, size: Int): Position? {
        val (dr, dc) = dir.delta()
        val next = Position(pos.row + dr, pos.col + dc)
        return if (next.isWithin(size)) next else null
    }
}
