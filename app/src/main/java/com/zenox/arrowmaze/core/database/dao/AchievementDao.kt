package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.AchievementProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [AchievementProgressEntity].
 */
@Dao
interface AchievementDao {

    @Upsert
    suspend fun upsert(entity: AchievementProgressEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AchievementProgressEntity>)

    @Query("SELECT * FROM achievement_progress WHERE achievementId = :id")
    suspend fun get(id: String): AchievementProgressEntity?

    @Query("SELECT * FROM achievement_progress")
    fun observeAll(): Flow<List<AchievementProgressEntity>>

    @Query("SELECT * FROM achievement_progress WHERE unlocked = 1")
    fun observeUnlocked(): Flow<List<AchievementProgressEntity>>

    @Query("SELECT * FROM achievement_progress WHERE unlocked = 1")
    suspend fun getUnlocked(): List<AchievementProgressEntity>

    @Query("UPDATE achievement_progress SET progressInt = :progress WHERE achievementId = :id")
    suspend fun setProgress(id: String, progress: Int)

    @Query("UPDATE achievement_progress SET unlocked = 1, unlockedAtEpochMs = :epochMs WHERE achievementId = :id")
    suspend fun markUnlocked(id: String, epochMs: Long)

    @Query("DELETE FROM achievement_progress WHERE achievementId = :id")
    suspend fun delete(id: String)
}
