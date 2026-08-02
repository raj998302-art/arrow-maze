package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.DailyChallengeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [DailyChallengeEntity].
 */
@Dao
interface DailyChallengeDao {

    @Upsert
    suspend fun upsert(entity: DailyChallengeEntity)

    @Query("SELECT * FROM daily_challenges WHERE dateIso = :dateIso")
    suspend fun getByDate(dateIso: String): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges WHERE dateIso = :dateIso")
    fun observeByDate(dateIso: String): Flow<DailyChallengeEntity?>

    @Query("SELECT * FROM daily_challenges ORDER BY dateIso DESC")
    fun observeAll(): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges ORDER BY dateIso DESC LIMIT 1")
    suspend fun getLatest(): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges WHERE completed = 1 ORDER BY dateIso DESC")
    suspend fun getCompleted(): List<DailyChallengeEntity>

    @Query("UPDATE daily_challenges SET completed = 1, solvedInSeconds = :solvedInSeconds, streakAfter = :streakAfter WHERE dateIso = :dateIso")
    suspend fun markCompleted(dateIso: String, solvedInSeconds: Int, streakAfter: Int)

    @Query("DELETE FROM daily_challenges WHERE dateIso = :dateIso")
    suspend fun delete(dateIso: String)
}
