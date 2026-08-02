package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of [com.zenox.arrowmaze.core.domain.model.GameStats]. There is one
 * row per user (the local device only ever stores the current user's row; remote
 * merges happen on Firestore sync). [solveTimesByLevel] is stored as a JSON
 * string and round-tripped via [com.zenox.arrowmaze.core.database.Converters].
 */
@Entity(tableName = "stats")
data class StatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")                 val uid: String,
    @ColumnInfo(name = "totalGames")          val totalGames: Int,
    @ColumnInfo(name = "totalWins")           val totalWins: Int,
    @ColumnInfo(name = "totalLosses")         val totalLosses: Int,
    @ColumnInfo(name = "totalTimeMs")         val totalTimeMs: Long,
    @ColumnInfo(name = "totalMoves")          val totalMoves: Int,
    @ColumnInfo(name = "totalHintsUsed")      val totalHintsUsed: Int,
    @ColumnInfo(name = "fastestSolveMs")      val fastestSolveMs: Long,
    @ColumnInfo(name = "bestStreak")          val bestStreak: Int,
    @ColumnInfo(name = "currentStreak")       val currentStreak: Int,
    @ColumnInfo(name = "averageSolveTimeMs")  val averageSolveTimeMs: Long,
    @ColumnInfo(name = "winRate")             val winRate: Float,
    @ColumnInfo(name = "solveTimesByLevel")   val solveTimesByLevel: String,
)
