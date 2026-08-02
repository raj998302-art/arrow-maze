package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [FriendEntity].
 */
@Dao
interface FriendDao {

    @Upsert
    suspend fun upsert(entity: FriendEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FriendEntity>)

    @Query("SELECT * FROM friends WHERE uid = :uid")
    suspend fun get(uid: String): FriendEntity?

    @Query("SELECT * FROM friends ORDER BY displayName ASC")
    suspend fun getAll(): List<FriendEntity>

    @Query("SELECT * FROM friends ORDER BY displayName ASC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE status = :status ORDER BY displayName ASC")
    fun observeByStatus(status: String): Flow<List<FriendEntity>>

    @Query("DELETE FROM friends WHERE uid = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM friends")
    suspend fun deleteAll()
}
