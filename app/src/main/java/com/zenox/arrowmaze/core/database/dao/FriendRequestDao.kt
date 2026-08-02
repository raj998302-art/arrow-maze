package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.FriendRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [FriendRequestEntity]. Incoming vs outgoing is determined
 * by comparing [FriendRequestEntity.fromUid] / [toUid] to the current user's
 * uid.
 */
@Dao
interface FriendRequestDao {

    @Upsert
    suspend fun upsert(entity: FriendRequestEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FriendRequestEntity>)

    @Query("SELECT * FROM friend_requests WHERE id = :id")
    suspend fun get(id: String): FriendRequestEntity?

    @Query("SELECT * FROM friend_requests WHERE toUid = :uid ORDER BY timestampEpochMs DESC")
    suspend fun getIncoming(uid: String): List<FriendRequestEntity>

    @Query("SELECT * FROM friend_requests WHERE fromUid = :uid ORDER BY timestampEpochMs DESC")
    suspend fun getOutgoing(uid: String): List<FriendRequestEntity>

    @Query("SELECT * FROM friend_requests WHERE toUid = :uid ORDER BY timestampEpochMs DESC")
    fun observeIncoming(uid: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE fromUid = :uid ORDER BY timestampEpochMs DESC")
    fun observeOutgoing(uid: String): Flow<List<FriendRequestEntity>>

    @Query("DELETE FROM friend_requests WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM friend_requests")
    suspend fun deleteAll()
}
