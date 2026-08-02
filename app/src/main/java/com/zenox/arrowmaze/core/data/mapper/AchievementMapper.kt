package com.zenox.arrowmaze.core.data.mapper

import com.zenox.arrowmaze.core.data.dto.AchievementProgressDto
import com.zenox.arrowmaze.core.database.entity.AchievementProgressEntity

/**
 * Mappers for achievement progress.
 *
 * The Achievement *definition* itself comes from a static in-app catalogue
 * (see `AchievementRepositoryImpl.buildCatalog`); nothing about it is persisted
 * in Room or Firestore. Only the per-player progress is, and this mapper owns
 * the entity ↔ dto conversion.
 */
object AchievementMapper {

    fun AchievementProgressEntity.toDto(): AchievementProgressDto = AchievementProgressDto(
        achievementId = achievementId,
        unlocked = unlocked,
        unlockedAt = unlockedAtEpochMs,
        progress = progressInt,
    )

    fun AchievementProgressDto.toEntity(): AchievementProgressEntity = AchievementProgressEntity(
        achievementId = achievementId,
        unlocked = unlocked,
        unlockedAtEpochMs = unlockedAt,
        progressInt = progress,
    )

    @JvmName("entitiesToDtoList")
    fun List<AchievementProgressEntity>.toDtoList(): List<AchievementProgressDto> = map { it.toDto() }
    @JvmName("dtosToEntityList")
    fun List<AchievementProgressDto>.toEntityList(): List<AchievementProgressEntity> = map { it.toEntity() }
}
