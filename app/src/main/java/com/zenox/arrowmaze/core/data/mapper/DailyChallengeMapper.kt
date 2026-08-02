package com.zenox.arrowmaze.core.data.mapper

import com.zenox.arrowmaze.core.database.entity.DailyChallengeEntity
import com.zenox.arrowmaze.core.domain.model.DailyChallenge
import com.zenox.arrowmaze.core.domain.model.DifficultyTier

/**
 * Bidirectional mappers between [DailyChallenge] (domain) and
 * [DailyChallengeEntity] (Room). The [DifficultyTier] enum is stored as its
 * name string in Room.
 *
 * No DTO is needed because daily challenges are deterministic per date and
 * generated locally; they're only persisted client-side for the player's
 * own completion history. (A server-side mirror can be added in Phase 10.)
 */
object DailyChallengeMapper {

    fun DailyChallengeEntity.toDomain(): DailyChallenge {
        val tier = runCatching { DifficultyTier.valueOf(tier) }
            .getOrElse { DifficultyTier.EASY }
        return DailyChallenge(
            dateIso = dateIso,
            seed = seed,
            tier = tier,
            boardSize = boardSize,
            completed = completed,
            rewardCoins = rewardCoins,
            rewardXp = rewardXp,
            solvedInSeconds = solvedInSeconds,
            streakAfter = streakAfter,
        )
    }

    fun DailyChallenge.toEntity(): DailyChallengeEntity = DailyChallengeEntity(
        dateIso = dateIso,
        seed = seed,
        tier = tier.name,
        boardSize = boardSize,
        completed = completed,
        rewardCoins = rewardCoins,
        rewardXp = rewardXp,
        solvedInSeconds = solvedInSeconds,
        streakAfter = streakAfter,
    )

    @JvmName("entitiesToDomainList")
    fun List<DailyChallengeEntity>.toDomainList(): List<DailyChallenge> = map { it.toDomain() }
    @JvmName("domainsToEntityList")
    fun List<DailyChallenge>.toEntityList(): List<DailyChallengeEntity> = map { it.toEntity() }
}
