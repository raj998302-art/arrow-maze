package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of one in-flight game. The [com.zenox.arrowmaze.core.domain.engine.GameEngine]
 * is the only thing that produces these — every move or hint returns a brand-new copy.
 *
 * @property board       Frozen board state after the most recent action.
 * @property config      Level parameters (size, tier, target arrow count, …).
 * @property movesPlayed Number of player moves applied so far (hints excluded).
 * @property hintsUsed   Number of hints consumed so far.
 * @property elapsedMs   Wall-clock time since the session started, in milliseconds.
 * @property isWon       True once [WinDetector] has confirmed a solution path.
 * @property isLost      True once the move cap was hit without a solution.
 * @property path        Current partial path traced from [Board.start]. May be empty / partial.
 *                       When [isWon] is true this is the full solution path.
 */
@Serializable
data class GameSession(
    @SerialName("board")       val board: Board,
    @SerialName("config")      val config: LevelConfig,
    @SerialName("movesPlayed") val movesPlayed: Int = 0,
    @SerialName("hintsUsed")   val hintsUsed: Int = 0,
    @SerialName("elapsedMs")   val elapsedMs: Long = 0L,
    @SerialName("isWon")       val isWon: Boolean = false,
    @SerialName("isLost")      val isLost: Boolean = false,
    @SerialName("path")        val path: List<Position> = emptyList()
) {

    /** True when no further moves can be played (won or lost). */
    val isTerminal: Boolean get() = isWon || isLost

    /** Number of moves remaining before [LoseReason.OUT_OF_MOVES] kicks in. */
    fun movesRemaining(maxMoves: Int = config.suggestedMaxMoves): Int =
        (maxMoves - movesPlayed).coerceAtLeast(0)

    override fun toString(): String =
        "GameSession(level=${config.level}, moves=$movesPlayed, hints=$hintsUsed, " +
            "won=$isWon, lost=$isLost, pathLen=${path.size})"
}
