package com.zenox.arrowmaze.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zenox.arrowmaze.core.database.entity.StatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [StatsEntity]. Includes targeted increment queries that
 * avoid rewriting the whole row on every game.
 */
@Dao
interface StatsDao {

    @Upsert
    suspend fun upsert(stats: StatsEntity)

    @Query("SELECT * FROM stats WHERE uid = :uid")
    suspend fun get(uid: String): StatsEntity?

    @Query("SELECT * FROM stats WHERE uid = :uid")
    fun observe(uid: String): Flow<StatsEntity?>

    @Query(
        """
        UPDATE stats
        SET totalGames = totalGames + 1,
            totalWins = totalWins + :won,
            totalLosses = totalLosses + :lost
        WHERE uid = :uid
        """
    )
    suspend fun incrementGames(uid: String, won: Int, lost: Int)

    @Query(
        """
        UPDATE stats
        SET totalTimeMs = totalTimeMs + :timeMs,
            totalMoves = totalMoves + :moves,
            totalHintsUsed = totalHintsUsed + :hintsUsed
        WHERE uid = :uid
        """
    )
    suspend fun addPlayTimeAndMoves(uid: String, timeMs: Long, moves: Int, hintsUsed: Int)

    @Query(
        """
        UPDATE stats
        SET fastestSolveMs = CASE
            WHEN fastestSolveMs = 0 OR :solveMs < fastestSolveMs THEN :solveMs
            ELSE fastestSolveMs
        END
        WHERE uid = :uid
        """
    )
    suspend fun updateFastestSolve(uid: String, solveMs: Long)

    @Query(
        """
        UPDATE stats
        SET bestStreak = CASE WHEN :streak > bestStreak THEN :streak ELSE bestStreak END,
            currentStreak = :streak
        WHERE uid = :uid
        """
    )
    suspend fun updateStreak(uid: String, streak: Int)

    @Query("DELETE FROM stats WHERE uid = :uid")
    suspend fun delete(uid: String)
}
