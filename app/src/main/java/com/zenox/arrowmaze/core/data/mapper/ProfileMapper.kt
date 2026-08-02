package com.zenox.arrowmaze.core.data.mapper

import com.zenox.arrowmaze.core.data.dto.ProfileDto
import com.zenox.arrowmaze.core.database.entity.ProfileEntity
import com.zenox.arrowmaze.core.domain.model.Profile
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Bidirectional mappers between [Profile] (domain), [ProfileEntity] (Room),
 * and [ProfileDto] (Firestore).
 *
 * Room stores the `List<String>` fields as JSON strings (via the local [json]
 * instance here, NOT via [com.zenox.arrowmaze.core.database.Converters] —
 * those TypeConverters are for direct `List<String>` Room columns; the entity
 * holds raw `String` columns and the mapper owns the encode/decode).
 */
object ProfileMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val listStringSerializer = ListSerializer(String.serializer())

    // ---------- Entity ↔ Domain ----------

    fun ProfileEntity.toDomain(): Profile {
        val owned = decodeList(ownedItems)
        val unlocked = decodeList(unlockedAchievements)
        return Profile(
            uid = uid,
            isGuest = isGuest,
            email = email,
            displayName = displayName,
            playerName = playerName,
            avatarUrl = avatarUrl,
            country = country,
            joinDateEpochMs = joinDateEpochMs,
            level = level,
            xp = xp,
            coins = coins,
            hints = hints,
            lives = lives,
            lastLifeRegenEpochMs = lastLifeRegenEpochMs,
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            bestStreak = bestStreak,
            currentStreak = currentStreak,
            averageSolveTimeMs = averageSolveTimeMs,
            highestLevel = highestLevel,
            currentThemeId = currentThemeId,
            currentArrowSkinId = currentArrowSkinId,
            currentTrailFxId = currentTrailFxId,
            ownedItems = owned,
            unlockedAchievements = unlocked,
            isPremium = isPremium,
            isVip = isVip,
        )
    }

    fun Profile.toEntity(): ProfileEntity {
        return ProfileEntity(
            uid = uid,
            isGuest = isGuest,
            email = email,
            displayName = displayName,
            playerName = playerName,
            avatarUrl = avatarUrl,
            country = country,
            joinDateEpochMs = joinDateEpochMs,
            level = level,
            xp = xp,
            coins = coins,
            hints = hints,
            lives = lives,
            lastLifeRegenEpochMs = lastLifeRegenEpochMs,
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            bestStreak = bestStreak,
            currentStreak = currentStreak,
            averageSolveTimeMs = averageSolveTimeMs,
            highestLevel = highestLevel,
            currentThemeId = currentThemeId,
            currentArrowSkinId = currentArrowSkinId,
            currentTrailFxId = currentTrailFxId,
            ownedItems = encodeList(ownedItems),
            unlockedAchievements = encodeList(unlockedAchievements),
            isPremium = isPremium,
            isVip = isVip,
        )
    }

    // ---------- DTO ↔ Domain ----------

    fun ProfileDto.toDomain(): Profile = Profile(
        uid = uid,
        isGuest = isGuest,
        email = email,
        displayName = displayName,
        playerName = playerName,
        avatarUrl = avatarUrl,
        country = country,
        joinDateEpochMs = joinDateEpochMs,
        level = level,
        xp = xp,
        coins = coins,
        hints = hints,
        lives = lives,
        lastLifeRegenEpochMs = lastLifeRegenEpochMs,
        gamesPlayed = gamesPlayed,
        gamesWon = gamesWon,
        bestStreak = bestStreak,
        currentStreak = currentStreak,
        averageSolveTimeMs = averageSolveTimeMs,
        highestLevel = highestLevel,
        currentThemeId = currentThemeId,
        currentArrowSkinId = currentArrowSkinId,
        currentTrailFxId = currentTrailFxId,
        ownedItems = ownedItems,
        unlockedAchievements = unlockedAchievements,
        isPremium = isPremium,
        isVip = isVip,
    )

    fun Profile.toDto(): ProfileDto = ProfileDto(
        uid = uid,
        isGuest = isGuest,
        email = email,
        displayName = displayName,
        playerName = playerName,
        avatarUrl = avatarUrl,
        country = country,
        joinDateEpochMs = joinDateEpochMs,
        level = level,
        xp = xp,
        coins = coins,
        hints = hints,
        lives = lives,
        lastLifeRegenEpochMs = lastLifeRegenEpochMs,
        gamesPlayed = gamesPlayed,
        gamesWon = gamesWon,
        bestStreak = bestStreak,
        currentStreak = currentStreak,
        averageSolveTimeMs = averageSolveTimeMs,
        highestLevel = highestLevel,
        currentThemeId = currentThemeId,
        currentArrowSkinId = currentArrowSkinId,
        currentTrailFxId = currentTrailFxId,
        ownedItems = ownedItems,
        unlockedAchievements = unlockedAchievements,
        isPremium = isPremium,
        isVip = isVip,
    )

    // ---------- helpers ----------

    private fun encodeList(values: List<String>): String =
        json.encodeToString(listStringSerializer, values)

    private fun decodeList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(listStringSerializer, value) }
            .getOrElse { emptyList() }
    }
}
