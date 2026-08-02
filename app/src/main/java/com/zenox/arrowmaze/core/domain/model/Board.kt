package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Immutable Arrow Maze board: an N×N grid of [Cell]s with a designated [start] and [goal].
 *
 * The board is pure data — every mutation (e.g. rotating an arrow) returns a brand-new
 * [Board] instance so the engine layer can replay moves safely and the UI can diff snapshots.
 *
 * Internally [cells] is stored as `List<List<Cell>>` for clarity: `cells[row][col]`.
 * Helpers [cellAt] / [withArrowRotated] / [arrowCells] / [validate] cover the operations
 * the engine and generator need.
 */
@Serializable
data class Board(
    @SerialName("size")  val size: Int,
    @SerialName("cells") val cells: List<List<Cell>>,
    @SerialName("start") val start: Position,
    @SerialName("goal")  val goal: Position
) {

    init {
        require(size > 0) { "Board size must be positive, was $size" }
        require(cells.size == size) {
            "cells outer size ${cells.size} does not match declared size $size"
        }
        cells.forEachIndexed { rowIndex, row ->
            require(row.size == size) {
                "Row $row has length ${row.size}, expected $size"
            }
        }
    }

    /** Returns the cell at ([row], [col]) or throws if out of bounds. */
    fun cellAt(row: Int, col: Int): Cell = cells[row][col]

    /** Returns the cell at [pos] or throws if out of bounds. */
    fun cellAt(pos: Position): Cell = cells[pos.row][pos.col]

    /** Returns the cell at [pos] or `null` if out of bounds (defensive lookup). */
    fun cellAtOrNull(pos: Position): Cell? =
        if (pos.isWithin(size)) cells[pos.row][pos.col] else null

    /**
     * Returns a new [Board] with the [ArrowCell] at [at] rotated 90° clockwise.
     * If the target cell is not an [ArrowCell] (start / goal / empty), the board is returned
     * unchanged — this matches the in-game UX where only arrows respond to taps.
     */
    fun withArrowRotated(at: Position): Board {
        if (!at.isWithin(size)) return this
        val current = cells[at.row][at.col]
        if (current !is Cell.ArrowCell) return this
        val rotated = current.rotatedCW()
        val newRows = cells.mapIndexed { rowIndex, row ->
            if (rowIndex != at.row) row
            else row.mapIndexed { colIndex, cell ->
                if (colIndex == at.col) rotated else cell
            }
        }
        return copy(cells = newRows)
    }

    /**
     * Same as [withArrowRotated] but rotates [times] × 90° clockwise. Useful for the
     * hint system (which tries all three non-trivial rotations).
     */
    fun withArrowRotatedBy(at: Position, times: Int): Board {
        if (!at.isWithin(size)) return this
        val current = cells[at.row][at.col]
        if (current !is Cell.ArrowCell) return this
        val rotated = current.rotated(times)
        val newRows = cells.mapIndexed { rowIndex, row ->
            if (rowIndex != at.row) row
            else row.mapIndexed { colIndex, cell ->
                if (colIndex == at.col) rotated else cell
            }
        }
        return copy(cells = newRows)
    }

    /** Returns every arrow cell on the board with its [Position]. */
    fun arrowCells(): List<Pair<Position, Cell.ArrowCell>> = buildList {
        cells.forEachIndexed { r, row ->
            row.forEachIndexed { c, cell ->
                if (cell is Cell.ArrowCell) add(Position(r, c) to cell)
            }
        }
    }

    /** Total number of cells (always [size]² for a valid board). */
    val cellCount: Int get() = size * size

    /**
     * Structural sanity check used by persistence / network layers before trusting a board.
     *  - The grid is exactly [size]×[size].
     *  - Exactly one [Cell.StartCell] and one [Cell.GoalCell] exist.
     *  - [start] / [goal] point at those cells.
     */
    fun validate(): Boolean {
        if (cells.size != size) return false
        for (row in cells) if (row.size != size) return false
        var starts = 0
        var goals = 0
        for ((r, row) in cells.withIndex()) {
            for ((c, cell) in row.withIndex()) {
                when (cell) {
                    is Cell.StartCell -> {
                        starts++
                        if (Position(r, c) != start) return false
                    }
                    is Cell.GoalCell -> {
                        goals++
                        if (Position(r, c) != goal) return false
                    }
                    else -> Unit
                }
            }
        }
        return starts == 1 && goals == 1
    }

    /** True when [pos] is on the outer ring of the board. */
    fun isOnEdge(pos: Position): Boolean =
        pos.row == 0 || pos.col == 0 || pos.row == size - 1 || pos.col == size - 1

    override fun toString(): String =
        "Board(size=$size, start=$start, goal=$goal, arrows=${arrowCells().size})"
}
