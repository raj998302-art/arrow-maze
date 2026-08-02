package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of [com.zenox.arrowmaze.core.domain.model.Friend].
 * Primary key is the friend's uid (one row per friend relationship).
 *
 * [status] is stored as the FriendStatus enum name (ACCEPTED / PENDING_SENT /
 * PENDING_RECEIVED / BLOCKED) and converted in the mapper.
 */
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")               val uid: String,
    @ColumnInfo(name = "playerName")        val playerName: String,
    @ColumnInfo(name = "displayName")       val displayName: String,
    @ColumnInfo(name = "avatarUrl")         val avatarUrl: String?,
    @ColumnInfo(name = "country")           val country: String,
    @ColumnInfo(name = "level")             val level: Int,
    @ColumnInfo(name = "xp")                val xp: Int,
    @ColumnInfo(name = "coins")             val coins: Int,
    @ColumnInfo(name = "isOnline")          val isOnline: Boolean,
    @ColumnInfo(name = "lastSeenEpochMs")   val lastSeenEpochMs: Long,
    @ColumnInfo(name = "status")            val status: String,
)
