package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.LevelProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [LevelProgressEntity].
 */
@Dao
interface LevelProgressDao {

    @Upsert
    suspend fun upsert(entity: LevelProgressEntity)

    @Query("SELECT * FROM level_progress WHERE level = :level")
    suspend fun getByLevel(level: Int): LevelProgressEntity?

    @Query("SELECT * FROM level_progress WHERE level = :level")
    fun observeByLevel(level: Int): Flow<LevelProgressEntity?>

    @Query("SELECT * FROM level_progress ORDER BY level ASC")
    fun observeAll(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress ORDER BY level ASC")
    suspend fun getAll(): List<LevelProgressEntity>

    @Query("SELECT MAX(level) FROM level_progress WHERE completed = 1")
    suspend fun highestCompletedLevel(): Int?

    @Query("SELECT COUNT(*) FROM level_progress WHERE completed = 1")
    suspend fun completedCount(): Int

    @Query("DELETE FROM level_progress WHERE level = :level")
    suspend fun delete(level: Int)
}
