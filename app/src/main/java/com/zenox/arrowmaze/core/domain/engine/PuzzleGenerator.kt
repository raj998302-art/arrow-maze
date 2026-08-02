package com.zenox.arrowmaze.core.domain.engine

import com.zenox.arrowmaze.core.domain.model.ArrowDirection
import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.Cell
import com.zenox.arrowmaze.core.domain.model.LevelConfig
import com.zenox.arrowmaze.core.domain.model.Position
import kotlin.random.Random

/**
 * Deterministic Arrow Maze board factory.
 *
 * ## Algorithm
 *
 *  1. Pick a start position on a board edge (never in a corner) and a goal position on the
 *     opposite edge (also not in a corner). The opposite-edge constraint guarantees the
 *     implicit start direction (see [WinDetector.entryDirectionFor]) points *into* the board
 *     and the path has room to meander.
 *
 *  2. Carve a solution path via a random walk with backtracking. The first step is forced
 *     by the start's implicit direction; from there the walk picks random unvisited
 *     neighbours, backtracking out of dead-ends, until it reaches the goal. A step cap
 *     bounds the worst case.
 *
 *  3. Lay down [Cell.ArrowCell]s on the interior path cells (every cell between start and
 *     goal) pointing along the path. Fill non-path cells with random [Cell.ArrowCell]
 *     decoys up to [LevelConfig.arrowCount]; any leftover non-path cells become
 *     [Cell.EmptyCell]s. The result is the **solved** board.
 *
 *  4. Verify the solved board with [WinDetector] — bail (and let the outer loop retry with
 *     a fresh sub-seed) if the carve somehow produced an unsolved board.
 *
 *  5. Randomly rotate path arrows by 90°, 180° or 270° CW so the puzzle is *broken* but
 *     solvable by rotating them back. Verify the broken board is **not** solved by
 *     [WinDetector] — bail if the rotations coincidentally produced an alternate solution.
 *
 *  6. Return the broken board. If every attempt fails, fall back to a trivial straight-line
 *     board so the caller always gets *something* solvable.
 *
 * The whole generator is pure and deterministic in [seed] — the same seed + config always
 * yields the same board, which is what powers Daily Challenge parity across devices.
 */
object PuzzleGenerator {

    /** Outer-loop attempts before falling back to the trivial board. */
    private const val MAX_ATTEMPTS = 24

    /** Probability that any given path arrow gets rotated when breaking the puzzle. */
    private const val ROTATION_PROBABILITY = 0.78f

    /** Minimum path length (start + at least 2 arrows + goal) so the puzzle is non-trivial. */
    private const val MIN_PATH_LENGTH = 4

    /** Per-attempt step cap for the carving random walk. */
    private const val CARVE_STEP_CAP_FACTOR = 8

    /**
     * Generates a solvable-but-broken [Board] for [config]. Deterministic in [seed].
     */
    fun generate(config: LevelConfig, seed: Long = System.currentTimeMillis()): Board {
        val baseRng = Random(seed)
        repeat(MAX_ATTEMPTS) {
            // Derive a fresh sub-seed per attempt so each retry explores a different carve.
            val subSeed = baseRng.nextLong()
            val attemptRng = Random(subSeed)
            val board = tryGenerateOnce(config, attemptRng)
            if (board != null) return board
        }
        // Last-resort: deterministic trivial solvable board.
        return trivialSolvableBoard(config, seed)
    }

    // ---------------------------------------------------------------------------------------------
    // Single attempt
    // ---------------------------------------------------------------------------------------------

    private fun tryGenerateOnce(config: LevelConfig, rng: Random): Board? {
        val size = config.boardSize

        // 1. Start / goal on opposite edges, not in corners.
        val (start, goal) = pickStartAndGoal(size, rng)

        // 2. Carve the solution path.
        val path = carvePath(start, goal, size, rng) ?: return null
        if (path.size < MIN_PATH_LENGTH) return null

        // 3. Build the solved board (path arrows + decoy arrows + empties).
        val solvedBoard = buildSolvedBoard(size, start, goal, path, config, rng)

        // 4. Sanity check: the carved board must be solved.
        if (!WinDetector.isSolved(solvedBoard)) return null

        // 5. Break the path with random rotations.
        val brokenBoard = breakPath(solvedBoard, path, rng)

        // 6. The broken board must NOT be solved — otherwise the player has nothing to do.
        if (WinDetector.isSolved(brokenBoard)) return null

        // Structural validation (defensive — should always pass given how we built it).
        if (!brokenBoard.validate()) return null

        return brokenBoard
    }

    // ---------------------------------------------------------------------------------------------
    // Start / goal placement
    // ---------------------------------------------------------------------------------------------

    /**
     * Picks (start, goal) on opposite edges. Edges are coded as 0=top, 1=bottom, 2=left,
     * 3=right; the opposite edge is (edge ^ 1). Both positions avoid corners by sampling the
     * inner range `[1, size-2]` along the edge.
     */
    private fun pickStartAndGoal(size: Int, rng: Random): Pair<Position, Position> {
        require(size >= 4) { "Board size must be >= 4 for non-corner edges, was $size" }
        val edge = rng.nextInt(4)
        val opposite = edge xor 1
        return positionOnEdge(edge, size, rng) to positionOnEdge(opposite, size, rng)
    }

    private fun positionOnEdge(edge: Int, size: Int, rng: Random): Position {
        val inner = 1 + rng.nextInt(size - 2) // [1, size-2] — never a corner
        return when (edge) {
            0    -> Position(0, inner)           // top
            1    -> Position(size - 1, inner)    // bottom
            2    -> Position(inner, 0)           // left
            3    -> Position(inner, size - 1)    // right
            else -> error("unreachable edge $edge")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Path carving
    // ---------------------------------------------------------------------------------------------

    /**
     * Random walk with backtracking from [start] to [goal]. The first step is forced by the
     * start's implicit entry direction (see [WinDetector.entryDirectionFor]); the rest of the
     * walk picks uniformly random unvisited neighbours and backtracks on dead-ends.
     *
     * Returns the ordered list of positions from start (inclusive) to goal (inclusive), or
     * `null` if no path was found within the step cap.
     */
    private fun carvePath(start: Position, goal: Position, size: Int, rng: Random): List<Position>? {
        val startDir = WinDetector.entryDirectionFor(start, size) ?: return null
        val (dr, dc) = startDir.delta()
        val firstStep = Position(start.row + dr, start.col + dc)
        if (!firstStep.isWithin(size)) return null

        val visited = HashSet<Position>()
        visited.add(start)
        visited.add(firstStep)

        val stack = ArrayDeque<Position>()
        stack.addLast(firstStep)

        val stepCap = size * size * CARVE_STEP_CAP_FACTOR
        var steps = 0

        while (stack.isNotEmpty() && steps < stepCap) {
            steps++
            val current = stack.last()
            if (current == goal) {
                return buildList {
                    add(start)
                    addAll(stack)
                }
            }
            val unvisited = current.neighborsWithin(size).filter { it !in visited }
            if (unvisited.isEmpty()) {
                stack.removeLast()
                continue
            }
            val next = unvisited.random(rng)
            visited.add(next)
            stack.addLast(next)
        }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // Board assembly
    // ---------------------------------------------------------------------------------------------

    /**
     * Builds the SOLVED board: start, goal, path arrows pointing along the path, decoy arrows
     * on non-path cells up to [LevelConfig.arrowCount], and [Cell.EmptyCell]s everywhere else.
     */
    private fun buildSolvedBoard(
        size: Int,
        start: Position,
        goal: Position,
        path: List<Position>,
        config: LevelConfig,
        rng: Random
    ): Board {
        // Initialise every cell as empty.
        val grid: MutableList<MutableList<Cell>> = MutableList(size) { MutableList(size) { Cell.EmptyCell } }

        // Place the fixed start and goal.
        grid[start.row][start.col] = Cell.StartCell
        grid[goal.row][goal.col] = Cell.GoalCell

        // Place path arrows: every interior path cell (index 1 .. size-2) gets an arrow
        // pointing to the next path cell.
        for (i in 1 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]
            grid[current.row][current.col] = Cell.ArrowCell(directionBetween(current, next))
        }

        // Fill decoy arrows on non-path cells up to the target arrow count.
        val pathSet = path.toHashSet()
        val pathArrowCount = (path.size - 2).coerceAtLeast(0)
        val decoysTarget = (config.arrowCount - pathArrowCount).coerceAtLeast(0)

        val decoySlots = mutableListOf<Position>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                val pos = Position(r, c)
                if (pos !in pathSet) decoySlots.add(pos)
            }
        }
        decoySlots.shuffle(rng)

        val decoysToPlace = minOf(decoysTarget, decoySlots.size)
        for (i in 0 until decoysToPlace) {
            val pos = decoySlots[i]
            grid[pos.row][pos.col] = Cell.ArrowCell(ArrowDirection.random(rng))
        }

        return Board(
            size = size,
            cells = grid.map { it.toList() },
            start = start,
            goal = goal
        )
    }

    /** Direction to step from [from] to its orthogonal neighbour [to]. */
    private fun directionBetween(from: Position, to: Position): ArrowDirection {
        val dr = to.row - from.row
        val dc = to.col - from.col
        return when {
            dr == -1 && dc == 0 -> ArrowDirection.UP
            dr == 1 && dc == 0 -> ArrowDirection.DOWN
            dr == 0 && dc == -1 -> ArrowDirection.LEFT
            dr == 0 && dc == 1 -> ArrowDirection.RIGHT
            else -> error("Non-adjacent positions in path: $from -> $to")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Path breaking
    // ---------------------------------------------------------------------------------------------

    /**
     * Randomly rotates interior path arrows by 90°, 180° or 270° CW so the puzzle is broken
     * but solvable by rotating them back. Always rotates at least one arrow so the board is
     * meaningfully different from [solvedBoard].
     */
    private fun breakPath(solvedBoard: Board, path: List<Position>, rng: Random): Board {
        val interior = path.drop(1).dropLast(1) // exclude start and goal
        if (interior.isEmpty()) return solvedBoard

        var current = solvedBoard
        val rotations = intArrayOf(1, 2, 3)
        var rotatedAny = false

        for (pos in interior) {
            if (rng.nextFloat() < ROTATION_PROBABILITY) {
                val times = rotations[rng.nextInt(rotations.size)]
                current = current.withArrowRotatedBy(pos, times)
                rotatedAny = true
            }
        }
        if (!rotatedAny) {
            // Ensure at least one rotation so the board is broken.
            current = current.withArrowRotatedBy(interior.first(), 1)
        }
        return current
    }

    // ---------------------------------------------------------------------------------------------
    // Trivial fallback
    // ---------------------------------------------------------------------------------------------

    /**
     * Last-resort board: a straight vertical corridor of DOWN arrows from a top-edge start to
     * a bottom-edge goal, plus a single rotated arrow that the player must correct. Always
     * solvable and always broken (in the gameplay sense).
     */
    private fun trivialSolvableBoard(config: LevelConfig, seed: Long): Board {
        val size = config.boardSize
        val mid = size / 2
        val start = Position(0, mid)
        val goal = Position(size - 1, mid)

        val grid: MutableList<MutableList<Cell>> = MutableList(size) { MutableList(size) { Cell.EmptyCell } }
        grid[start.row][start.col] = Cell.StartCell
        grid[goal.row][goal.col] = Cell.GoalCell
        for (r in 1 until size - 1) {
            grid[r][mid] = Cell.ArrowCell(ArrowDirection.DOWN)
        }

        // Scatter a few decoys so the board isn't suspiciously empty.
        val rng = Random(seed xor 0xDEADBEEFL)
        val decoySlots = mutableListOf<Position>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] is Cell.EmptyCell) decoySlots.add(Position(r, c))
            }
        }
        decoySlots.shuffle(rng)
        val decoysTarget = (config.arrowCount - (size - 2)).coerceAtLeast(0)
        val decoysToPlace = minOf(decoysTarget, decoySlots.size)
        for (i in 0 until decoysToPlace) {
            val pos = decoySlots[i]
            grid[pos.row][pos.col] = Cell.ArrowCell(ArrowDirection.random(rng))
        }

        // Sanity: confirm the all-DOWN corridor is solved, then rotate one arrow to break it.
        val solved = Board(size = size, cells = grid.map { it.toList() }, start = start, goal = goal)
        if (!WinDetector.isSolved(solved)) {
            // Pathological fallback-of-the-fallback: return the solved board anyway so the
            // caller always gets a valid (if uninteresting) board.
            return solved
        }
        val breakPos = Position(1, mid)
        val broken = solved.withArrowRotatedBy(breakPos, 1)
        // If breaking somehow still leaves the board solved, return the solved board.
        return if (WinDetector.isSolved(broken)) solved else broken
    }
}
