package com.zenox.arrowmaze.core.domain.engine

import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.Cell
import com.zenox.arrowmaze.core.domain.model.GameEvent
import com.zenox.arrowmaze.core.domain.model.GameSession
import com.zenox.arrowmaze.core.domain.model.LevelConfig
import com.zenox.arrowmaze.core.domain.model.LoseReason
import com.zenox.arrowmaze.core.domain.model.MoveResult
import com.zenox.arrowmaze.core.domain.model.Position

/**
 * Stateful driver for a single Arrow Maze play-through.
 *
 * The engine wraps an immutable [Board] + [LevelConfig] and exposes the only two actions the
 * player can take:
 *  - [rotateCell] — tap an arrow to rotate it 90° CW.
 *  - [useHint]    — consume a hint to auto-rotate the most useful arrow.
 *
 * Every action returns the most significant [GameEvent] it produced (or `null` if the action
 * was rejected, e.g. tapping a non-arrow cell or acting after the game has ended). The full
 * live state is always available via [session] and [currentPath].
 *
 * ## Move cap
 *
 * [maxMoves] defaults to `boardSize²` so the player can rotate every arrow at least once.
 * When [GameSession.movesPlayed] reaches [maxMoves] without a win, the next rotation emits
 * [GameEvent.Lost] with [LoseReason.OUT_OF_MOVES]. Hints do NOT consume moves.
 *
 * ## Win / lose detection
 *
 * After every rotation the engine runs [WinDetector.pathPrefix]; if the prefix ends at the
 * goal, the session is marked won and [GameEvent.Won] is emitted. The same prefix is exposed
 * via [currentPath] for the UI to render the live trail.
 *
 * The engine never mutates the input [Board]; every rotation produces a new immutable board
 * via [Board.withArrowRotated].
 */
class GameEngine(
    initialBoard: Board,
    val config: LevelConfig,
    val maxMoves: Int = config.boardSize * config.boardSize
) {

    /**
     * Live session snapshot. Mutated only by this engine via `copy(...)`. Exposed read-only
     * via [session] so observers can render HUD state without coupling to engine internals.
     */
    private var currentSession: GameSession

    init {
        require(maxMoves > 0) { "maxMoves must be positive, was $maxMoves" }
        require(initialBoard.size == config.boardSize) {
            "Initial board size ${initialBoard.size} does not match config boardSize ${config.boardSize}"
        }
        val initialPath = WinDetector.pathPrefix(initialBoard)
        val initiallySolved = initialPath.isNotEmpty() && initialPath.last() == initialBoard.goal
        currentSession = GameSession(
            board = initialBoard,
            config = config,
            movesPlayed = 0,
            hintsUsed = 0,
            elapsedMs = 0L,
            isWon = initiallySolved,
            isLost = false,
            path = initialPath
        )
    }

    /** Read-only snapshot of the live session. */
    val session: GameSession get() = currentSession

    /** Convenience: the current board. */
    val board: Board get() = currentSession.board

    /** Convenience: moves played so far. */
    val movesPlayed: Int get() = currentSession.movesPlayed

    /** Convenience: hints used so far. */
    val hintsUsed: Int get() = currentSession.hintsUsed

    /** Convenience: moves remaining before [LoseReason.OUT_OF_MOVES] kicks in. */
    fun movesRemaining(): Int = (maxMoves - currentSession.movesPlayed).coerceAtLeast(0)

    /** True once the game is over (won or lost). */
    fun isComplete(): Boolean = currentSession.isTerminal

    /** The current partial path traced from [Board.start]; may be empty, partial, or full. */
    fun currentPath(): List<Position> = currentSession.path

    /**
     * Rotates the arrow at [at] 90° CW. Returns the most significant event produced:
     *
     *  - [GameEvent.Won]  — the rotation completed a Start → Goal path.
     *  - [GameEvent.Lost] — the move cap was hit without a solution.
     *  - [GameEvent.CellRotated] — the move was applied and the game continues.
     *
     * Returns `null` (and leaves the session untouched) when:
     *  - the game is already terminal,
     *  - [at] is out of bounds,
     *  - [at] does not hold an [Cell.ArrowCell] (start/goal/empty are not rotatable).
     */
    fun rotateCell(at: Position): GameEvent? {
        // Refuse moves once the game is over.
        if (isComplete()) return null
        // Validate the target.
        if (!at.isWithin(currentSession.board.size)) return null
        val oldCell = currentSession.board.cellAt(at)
        if (oldCell !is Cell.ArrowCell) return null

        // Apply the rotation immutably.
        val newBoard = currentSession.board.withArrowRotated(at)
        val newDirection = (newBoard.cellAt(at) as Cell.ArrowCell).direction
        val newMovesPlayed = currentSession.movesPlayed + 1
        val newPath = WinDetector.pathPrefix(newBoard)
        val isSolved = newPath.isNotEmpty() && newPath.last() == newBoard.goal

        return when {
            isSolved -> {
                currentSession = currentSession.copy(
                    board = newBoard,
                    movesPlayed = newMovesPlayed,
                    isWon = true,
                    path = newPath
                )
                GameEvent.Won(path = newPath)
            }
            newMovesPlayed >= maxMoves -> {
                currentSession = currentSession.copy(
                    board = newBoard,
                    movesPlayed = newMovesPlayed,
                    isLost = true,
                    path = newPath
                )
                GameEvent.Lost(reason = LoseReason.OUT_OF_MOVES)
            }
            else -> {
                currentSession = currentSession.copy(
                    board = newBoard,
                    movesPlayed = newMovesPlayed,
                    path = newPath
                )
                GameEvent.CellRotated(at = at, newDirection = newDirection)
            }
        }
    }

    /**
     * Consumes a hint: finds the most useful arrow to rotate (via [HintSystem]) and applies
     * the rotation. Returns:
     *
     *  - [GameEvent.Won]     — the hint solved the puzzle.
     *  - [GameEvent.HintUsed] — the hint was applied and the game continues.
     *
     * Returns `null` when:
     *  - the game is already terminal,
     *  - no useful hint exists (the player is already as far along as any single rotation
     *    can take them).
     *
     * Hints do NOT consume moves; they only increment [GameSession.hintsUsed].
     */
    fun useHint(): GameEvent? {
        if (isComplete()) return null
        val hint = HintSystem.findHintWithRotation(currentSession.board) ?: return null
        val (at, times) = hint

        val newBoard = currentSession.board.withArrowRotatedBy(at, times)
        val newHintsUsed = currentSession.hintsUsed + 1
        val newPath = WinDetector.pathPrefix(newBoard)
        val isSolved = newPath.isNotEmpty() && newPath.last() == newBoard.goal

        return if (isSolved) {
            currentSession = currentSession.copy(
                board = newBoard,
                hintsUsed = newHintsUsed,
                isWon = true,
                path = newPath
            )
            GameEvent.Won(path = newPath)
        } else {
            currentSession = currentSession.copy(
                board = newBoard,
                hintsUsed = newHintsUsed,
                path = newPath
            )
            GameEvent.HintUsed(at = at)
        }
    }

    /**
     * Updates the session's elapsed-time counter. Intended to be called by an external timer
     * (e.g. a Compose `LaunchedEffect` ticking once per second) — the engine itself never
     * touches the clock so unit tests stay deterministic.
     */
    fun updateElapsed(elapsedMs: Long) {
        if (isComplete()) return
        currentSession = currentSession.copy(elapsedMs = elapsedMs)
    }

    /**
     * Marks the session as lost with [LoseReason.GAVE_UP]. Returns the emitted [GameEvent.Lost]
     * or `null` if the game was already terminal.
     */
    fun giveUp(): GameEvent.Lost? {
        if (isComplete()) return null
        currentSession = currentSession.copy(isLost = true)
        return GameEvent.Lost(reason = LoseReason.GAVE_UP)
    }

    /**
     * Returns the [MoveResult] equivalent of the current session state — useful for callers
     * that prefer the result-style API over events.
     */
    fun currentResult(): MoveResult = when {
        currentSession.isWon -> MoveResult.Won(
            board = currentSession.board,
            movesPlayed = currentSession.movesPlayed,
            pathFound = currentSession.path
        )
        currentSession.isLost -> MoveResult.Lost(
            board = currentSession.board,
            movesPlayed = currentSession.movesPlayed,
            reason = LoseReason.OUT_OF_MOVES
        )
        else -> MoveResult.Continue(
            board = currentSession.board,
            movesPlayed = currentSession.movesPlayed
        )
    }
}
