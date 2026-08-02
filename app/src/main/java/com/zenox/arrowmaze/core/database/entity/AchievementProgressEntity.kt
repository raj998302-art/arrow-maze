package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-achievement progress row. The achievement definition lives in a static
 * catalogue (see AchievementRepositoryImpl); this entity only persists the
 * player's per-achievement progress / unlock state.
 *
 * @property achievementId        Stable achievement id (matches the catalogue).
 * @property unlocked             Whether the achievement has been unlocked.
 * @property unlockedAtEpochMs    Wall-clock time of unlock; null if not yet unlocked.
 * @property progressInt          Integer progress toward the unlock condition
 *                                (e.g. count of solved levels for a "solve 50"
 *                                achievement). The achievements engine updates it.
 */
@Entity(tableName = "achievement_progress")
data class AchievementProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "achievementId")  val achievementId: String,
    @ColumnInfo(name = "unlocked")       val unlocked: Boolean,
    @ColumnInfo(name = "unlockedAtEpochMs") val unlockedAtEpochMs: Long?,
    @ColumnInfo(name = "progressInt")    val progressInt: Int,
)
