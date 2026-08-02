package com.zenox.arrowmaze.core.data.mapper

import com.zenox.arrowmaze.core.data.dto.StatsDto
import com.zenox.arrowmaze.core.database.entity.StatsEntity
import com.zenox.arrowmaze.core.domain.model.GameStats
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Bidirectional mappers between [GameStats] (domain), [StatsEntity] (Room),
 * and [StatsDto] (Firestore).
 *
 * `solveTimesByLevel: Map<Int, Long>` is stored as a JSON string in Room and
 * as a Firestore map natively. The Room JSON encoding uses the same [Json]
 * config as the rest of the data layer for cross-mapper compatibility.
 */
object StatsMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val mapSerializer = MapSerializer(Int.serializer(), Long.serializer())

    // ---------- Entity ↔ Domain ----------

    fun StatsEntity.toDomain(): GameStats {
        val map = decodeMap(solveTimesByLevel)
        return GameStats(
            totalGames = totalGames,
            totalWins = totalWins,
            totalLosses = totalLosses,
            totalTimeMs = totalTimeMs,
            totalMoves = totalMoves,
            totalHintsUsed = totalHintsUsed,
            fastestSolveMs = fastestSolveMs,
            bestStreak = bestStreak,
            currentStreak = currentStreak,
            averageSolveTimeMs = averageSolveTimeMs,
            winRate = winRate,
            solveTimesByLevel = map,
        )
    }

    fun GameStats.toEntity(uid: String): StatsEntity {
        return StatsEntity(
            uid = uid,
            totalGames = totalGames,
            totalWins = totalWins,
            totalLosses = totalLosses,
            totalTimeMs = totalTimeMs,
            totalMoves = totalMoves,
            totalHintsUsed = totalHintsUsed,
            fastestSolveMs = fastestSolveMs,
            bestStreak = bestStreak,
            currentStreak = currentStreak,
            averageSolveTimeMs = averageSolveTimeMs,
            winRate = winRate,
            solveTimesByLevel = encodeMap(solveTimesByLevel),
        )
    }

    // ---------- DTO ↔ Domain ----------

    fun StatsDto.toDomain(): GameStats = GameStats(
        totalGames = totalGames,
        totalWins = totalWins,
        totalLosses = totalLosses,
        totalTimeMs = totalTimeMs,
        totalMoves = totalMoves,
        totalHintsUsed = totalHintsUsed,
        fastestSolveMs = fastestSolveMs,
        bestStreak = bestStreak,
        currentStreak = currentStreak,
        averageSolveTimeMs = averageSolveTimeMs,
        winRate = winRate,
        solveTimesByLevel = solveTimesByLevel,
    )

    fun GameStats.toDto(): StatsDto = StatsDto(
        totalGames = totalGames,
        totalWins = totalWins,
        totalLosses = totalLosses,
        totalTimeMs = totalTimeMs,
        totalMoves = totalMoves,
        totalHintsUsed = totalHintsUsed,
        fastestSolveMs = fastestSolveMs,
        bestStreak = bestStreak,
        currentStreak = currentStreak,
        averageSolveTimeMs = averageSolveTimeMs,
        winRate = winRate,
        solveTimesByLevel = solveTimesByLevel,
    )

    // ---------- helpers ----------

    private fun encodeMap(map: Map<Int, Long>): String =
        json.encodeToString(mapSerializer, map)

    private fun decodeMap(value: String): Map<Int, Long> {
        if (value.isBlank()) return emptyMap()
        return runCatching { json.decodeFromString(mapSerializer, value) }
            .getOrElse { emptyMap() }
    }
}
