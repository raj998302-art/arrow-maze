package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Why a game ended in a loss. Surfaces as a UI string lookup key in the presentation layer.
 */
@Serializable
enum class LoseReason {
    @SerialName("OUT_OF_MOVES") OUT_OF_MOVES,
    @SerialName("GAVE_UP")      GAVE_UP
}

/**
 * Terminal outcome of a single move applied by the [com.zenox.arrowmaze.core.domain.engine.GameEngine].
 *
 * - [Continue] — the move was legal but did not win/lose; carry on.
 * - [Won]      — the path now reaches the goal; carries the solved path for the win animation.
 * - [Lost]     — the move cap was hit without a solution (or the player gave up).
 *
 * Each variant embeds the latest [Board] and [movesPlayed] so observers can render a fresh
 * snapshot without re-reading the engine.
 */
@Serializable
sealed interface MoveResult {

    val board: Board
    val movesPlayed: Int

    @Serializable
    @SerialName("continue")
    data class Continue(
        @SerialName("board")      override val board: Board,
        @SerialName("movesPlayed") override val movesPlayed: Int
    ) : MoveResult

    @Serializable
    @SerialName("won")
    data class Won(
        @SerialName("board")      override val board: Board,
        @SerialName("movesPlayed") override val movesPlayed: Int,
        @SerialName("pathFound")  val pathFound: List<Position>
    ) : MoveResult

    @Serializable
    @SerialName("lost")
    data class Lost(
        @SerialName("board")      override val board: Board,
        @SerialName("movesPlayed") override val movesPlayed: Int,
        @SerialName("reason")     val reason: LoseReason
    ) : MoveResult

    /** True when this result is terminal (no more moves accepted). */
    val isTerminal: Boolean get() = this is Won || this is Lost
}
