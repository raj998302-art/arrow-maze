package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-level best-result row. One row per level the player has attempted.
 *
 * @property level      1-based level index (primary key).
 * @property completed  Whether the level has ever been solved.
 * @property bestMoves  Fewest moves used across all completions (null if never completed).
 * @property bestTimeMs Fastest completion time in ms (null if never completed).
 * @property stars      Star rating (0–3) awarded on best completion.
 */
@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "level")      val level: Int,
    @ColumnInfo(name = "completed")  val completed: Boolean,
    @ColumnInfo(name = "bestMoves")  val bestMoves: Int?,
    @ColumnInfo(name = "bestTimeMs") val bestTimeMs: Long?,
    @ColumnInfo(name = "stars")      val stars: Int,
)
