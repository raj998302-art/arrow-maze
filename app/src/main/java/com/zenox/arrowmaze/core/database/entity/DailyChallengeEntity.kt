package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of [com.zenox.arrowmaze.core.domain.model.DailyChallenge].
 * Primary key is the ISO-8601 date string.
 */
@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey
    @ColumnInfo(name = "dateIso")           val dateIso: String,
    @ColumnInfo(name = "seed")              val seed: Long,
    @ColumnInfo(name = "tier")              val tier: String,
    @ColumnInfo(name = "boardSize")         val boardSize: Int,
    @ColumnInfo(name = "completed")         val completed: Boolean,
    @ColumnInfo(name = "rewardCoins")       val rewardCoins: Int,
    @ColumnInfo(name = "rewardXp")          val rewardXp: Int,
    @ColumnInfo(name = "solvedInSeconds")   val solvedInSeconds: Int?,
    @ColumnInfo(name = "streakAfter")       val streakAfter: Int,
)
