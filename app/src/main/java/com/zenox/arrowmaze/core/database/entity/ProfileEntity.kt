package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of [com.zenox.arrowmaze.core.domain.model.Profile].
 *
 * Lists ([Profile.ownedItems], [Profile.unlockedAchievements]) are stored as JSON
 * strings and decoded by [com.zenox.arrowmaze.core.database.Converters] — but
 * because these are `List<String>` and not fields Room inspects directly, the
 * entity itself holds them as plain `String` columns and the mappers serialise
 * them via `kotlinx.serialization`.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")                   val uid: String,
    @ColumnInfo(name = "isGuest")               val isGuest: Boolean,
    @ColumnInfo(name = "email")                 val email: String?,
    @ColumnInfo(name = "displayName")           val displayName: String,
    @ColumnInfo(name = "playerName")            val playerName: String,
    @ColumnInfo(name = "avatarUrl")             val avatarUrl: String?,
    @ColumnInfo(name = "country")               val country: String,
    @ColumnInfo(name = "joinDateEpochMs")       val joinDateEpochMs: Long,
    @ColumnInfo(name = "level")                 val level: Int,
    @ColumnInfo(name = "xp")                    val xp: Int,
    @ColumnInfo(name = "coins")                 val coins: Int,
    @ColumnInfo(name = "hints")                 val hints: Int,
    @ColumnInfo(name = "lives")                 val lives: Int,
    @ColumnInfo(name = "lastLifeRegenEpochMs")  val lastLifeRegenEpochMs: Long,
    @ColumnInfo(name = "gamesPlayed")           val gamesPlayed: Int,
    @ColumnInfo(name = "gamesWon")              val gamesWon: Int,
    @ColumnInfo(name = "bestStreak")            val bestStreak: Int,
    @ColumnInfo(name = "currentStreak")         val currentStreak: Int,
    @ColumnInfo(name = "averageSolveTimeMs")    val averageSolveTimeMs: Long,
    @ColumnInfo(name = "highestLevel")          val highestLevel: Int,
    @ColumnInfo(name = "currentThemeId")        val currentThemeId: String,
    @ColumnInfo(name = "currentArrowSkinId")    val currentArrowSkinId: String,
    @ColumnInfo(name = "currentTrailFxId")      val currentTrailFxId: String,
    @ColumnInfo(name = "ownedItems")            val ownedItems: String,
    @ColumnInfo(name = "unlockedAchievements")  val unlockedAchievements: String,
    @ColumnInfo(name = "isPremium")             val isPremium: Boolean,
    @ColumnInfo(name = "isVip")                 val isVip: Boolean,
)
