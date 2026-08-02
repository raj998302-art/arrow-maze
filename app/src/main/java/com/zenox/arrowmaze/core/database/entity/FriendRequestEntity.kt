package com.zenox.arrowmaze.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of [com.zenox.arrowmaze.core.domain.model.FriendRequest].
 * Primary key is the request id (Firestore document id).
 *
 * The entity stores both incoming and outgoing requests; the caller distinguishes
 * them by comparing [fromUid] / [toUid] to the current user's uid.
 */
@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")                val id: String,
    @ColumnInfo(name = "fromUid")           val fromUid: String,
    @ColumnInfo(name = "toUid")             val toUid: String,
    @ColumnInfo(name = "fromName")          val fromName: String,
    @ColumnInfo(name = "timestampEpochMs")  val timestampEpochMs: Long,
    @ColumnInfo(name = "message")           val message: String?,
)
