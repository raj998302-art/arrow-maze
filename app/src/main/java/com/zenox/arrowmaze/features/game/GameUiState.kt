package com.zenox.arrowmaze.features.game

import com.zenox.arrowmaze.core.domain.model.GameSession
import com.zenox.arrowmaze.core.domain.model.LevelConfig
import com.zenox.arrowmaze.core.domain.model.LoseReason
import com.zenox.arrowmaze.core.domain.model.Position

/**
 * Single source of truth for the Game screen's render state.
 *
 * The [GameViewModel] owns the only mutable copy; the Compose layer consumes
 * the emitted snapshots via `collectAsStateWithLifecycle()` and switches on
 * the variant to choose what to draw.
 *
 * States form a forward-only lifecycle (Loading → Playing → Won/Lost) with
 * `Error` reachable from any state when an unexpected exception bubbles out
 * of a repository call. `onRestart()` and `onContinueAfterWin()` are the only
 * valid forward edges out of terminal states.
 */
sealed interface GameUiState {

    /**
     * Initial state while the board is being generated and the player profile
     * is being loaded. Holds the requested [level] so the UI can render a
     * branded "Loading Level N" placeholder.
     */
    data class Loading(val level: Int) : GameUiState

    /**
     * Active play state.
     *
     * @property session           Frozen snapshot of the engine's session
     *                              (board + counters + partial path).
     * @property path              The current partial solution path traced
     *                              from [GameSession.board]'s start cell.
     *                              Same reference as [session.path] but
     *                              surfaced as a top-level field for ergonomic
     *                              Canvas consumption.
     * @property lastRotatedCell   The most recently rotated arrow cell — used
     *                              by [GameBoardCanvas] to drive the rotation
     *                              animation. `null` until the first tap.
     * @property coins             Live player coin balance (from profile).
     * @property hints             Live player hint balance (from profile).
     * @property lives             Live player life count (from profile).
     * @property config            The [LevelConfig] that produced this board.
     */
    data class Playing(
        val session: GameSession,
        val path: List<Position>,
        val lastRotatedCell: Position?,
        val coins: Int,
        val hints: Int,
        val lives: Int,
        val config: LevelConfig,
    ) : GameUiState

    /**
     * Terminal win state.
     *
     * @property session     Final session snapshot (isWon = true).
     * @property path        Full solution path from start to goal.
     * @property coinsEarned Coins granted to the player for this win.
     * @property xpEarned    XP granted to the player for this win.
     * @property timeMs      Wall-clock solve time in ms.
     * @property moves       Number of player moves used.
     */
    data class Won(
        val session: GameSession,
        val path: List<Position>,
        val coinsEarned: Int,
        val xpEarned: Int,
        val timeMs: Long,
        val moves: Int,
    ) : GameUiState

    /**
     * Terminal loss state.
     *
     * @property session Final session snapshot (isLost = true).
     * @property reason  Why the game ended (move cap or gave up).
     * @property coins   Live player coin balance — needed for the lose
     *                    dialog's "Use Hint" affordance.
     * @property hints   Live player hint balance.
     */
    data class Lost(
        val session: GameSession,
        val reason: LoseReason,
        val coins: Int,
        val hints: Int,
    ) : GameUiState

    /**
     * Unrecoverable error state. The player can retry via `onRestart()` which
     * re-enters [Loading].
     */
    data class Error(val message: String) : GameUiState
}
