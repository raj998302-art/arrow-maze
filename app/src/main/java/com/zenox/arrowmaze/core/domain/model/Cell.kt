package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discriminator enum for the [Cell] sealed hierarchy. Persisted alongside each cell so that
 * future schema migrations and cross-platform clients can identify cell kinds even if the
 * Kotlin class names change.
 */
@Serializable
enum class CellType {
    @SerialName("ARROW")  ARROW,
    @SerialName("START")  START,
    @SerialName("GOAL")   GOAL,
    @SerialName("EMPTY")  EMPTY
}

/**
 * A single square on the board. Sealed so that the engine can exhaustively `when` over the
 * four variants without falling through to a default branch.
 *
 * Variants:
 *  - [ArrowCell]   — a rotatable arrow the player can tap.
 *  - [StartCell]   — the fixed entry point of the path.
 *  - [GoalCell]    — the fixed exit the path must reach.
 *  - [EmptyCell]   — decorative gap; arrows cannot enter it.
 *
 * Every variant exposes a [type] tag so the model is self-describing on the wire.
 */
@Serializable
sealed interface Cell {

    val type: CellType

    @Serializable
    @SerialName("arrow")
    data class ArrowCell(
        @SerialName("direction") val direction: ArrowDirection
    ) : Cell {
        override val type: CellType get() = CellType.ARROW

        /** Returns a copy rotated 90° clockwise. */
        fun rotatedCW(): ArrowCell = copy(direction = direction.rotateCW())

        /** Returns a copy rotated 90° counter-clockwise. */
        fun rotatedCCW(): ArrowCell = copy(direction = direction.rotateCCW())

        /** Returns a copy rotated [times] × 90° clockwise. */
        fun rotated(times: Int): ArrowCell {
            val t = ((times % 4) + 4) % 4
            var c = this
            repeat(t) { c = c.rotatedCW() }
            return c
        }
    }

    @Serializable
    @SerialName("start")
    data object StartCell : Cell {
        override val type: CellType get() = CellType.START
    }

    @Serializable
    @SerialName("goal")
    data object GoalCell : Cell {
        override val type: CellType get() = CellType.GOAL
    }

    @Serializable
    @SerialName("empty")
    data object EmptyCell : Cell {
        override val type: CellType get() = CellType.EMPTY
    }

    companion object {
        /** True if this cell is a fixed (non-rotatable) cell — start/goal/empty. */
        fun Cell.isFixed(): Boolean = this !is ArrowCell
    }
}
