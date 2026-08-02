package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Immutable grid coordinate. Row grows downward, column grows rightward — matches
 * the [Board.cells] list-of-lists layout (`cells[row][col]`).
 */
@Serializable
data class Position(
    @SerialName("row") val row: Int,
    @SerialName("col") val col: Int
) {

    /** Returns a new [Position] shifted by the given (rowDelta, colDelta). */
    fun plus(delta: Pair<Int, Int>): Position = Position(row + delta.first, col + delta.second)

    /** Convenience overload accepting explicit deltas. */
    fun plus(rowDelta: Int, colDelta: Int): Position = Position(row + rowDelta, col + colDelta)

    /** True when this position is inside a square board of side [size]. */
    fun isWithin(size: Int): Boolean =
        row in 0 until size && col in 0 until size

    /** True when this position is inside an arbitrary rectangle. */
    fun isWithin(rows: Int, cols: Int): Boolean =
        row in 0 until rows && col in 0 until cols

    /** The four orthogonal neighbours in CW order starting from Up. */
    fun neighbors(): List<Position> = listOf(
        plus(-1, 0), // up
        plus(0, 1),  // right
        plus(1, 0),  // down
        plus(0, -1)  // left
    )

    /** Only the neighbours that fall inside a square board of side [size]. */
    fun neighborsWithin(size: Int): List<Position> = neighbors().filter { it.isWithin(size) }

    /** Manhattan distance to another position. */
    fun manhattanTo(other: Position): Int = kotlin.math.abs(row - other.row) + kotlin.math.abs(col - other.col)

    override fun toString(): String = "($row,$col)"
}
