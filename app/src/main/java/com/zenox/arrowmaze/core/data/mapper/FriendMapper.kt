package com.zenox.arrowmaze.core.data.mapper

import com.zenox.arrowmaze.core.data.dto.FriendDto
import com.zenox.arrowmaze.core.database.entity.FriendEntity
import com.zenox.arrowmaze.core.domain.model.Friend
import com.zenox.arrowmaze.core.domain.model.FriendStatus

/**
 * Bidirectional mappers between [Friend] (domain), [FriendEntity] (Room),
 * and [FriendDto] (Firestore). The [FriendStatus] enum is stored as its name
 * string in both layers (Firestore needs a primitive; Room prefers a primitive
 * too for query-ability).
 */
object FriendMapper {

    // ---------- Entity ↔ Domain ----------

    fun FriendEntity.toDomain(): Friend {
        val status = runCatching { FriendStatus.valueOf(status) }
            .getOrElse { FriendStatus.ACCEPTED }
        return Friend(
            uid = uid,
            playerName = playerName,
            displayName = displayName,
            avatarUrl = avatarUrl,
            country = country,
            level = level,
            xp = xp,
            coins = coins,
            isOnline = isOnline,
            lastSeenEpochMs = lastSeenEpochMs,
            status = status,
        )
    }

    fun Friend.toEntity(): FriendEntity = FriendEntity(
        uid = uid,
        playerName = playerName,
        displayName = displayName,
        avatarUrl = avatarUrl,
        country = country,
        level = level,
        xp = xp,
        coins = coins,
        isOnline = isOnline,
        lastSeenEpochMs = lastSeenEpochMs,
        status = status.name,
    )

    // ---------- DTO ↔ Domain ----------

    fun FriendDto.toDomain(): Friend {
        val status = runCatching { FriendStatus.valueOf(status) }
            .getOrElse { FriendStatus.ACCEPTED }
        return Friend(
            uid = uid,
            playerName = playerName,
            displayName = displayName,
            avatarUrl = avatarUrl,
            country = country,
            level = level,
            xp = xp,
            coins = coins,
            isOnline = isOnline,
            lastSeenEpochMs = lastSeenEpochMs,
            status = status,
        )
    }

    fun Friend.toDto(): FriendDto = FriendDto(
        uid = uid,
        playerName = playerName,
        displayName = displayName,
        avatarUrl = avatarUrl,
        country = country,
        level = level,
        xp = xp,
        coins = coins,
        isOnline = isOnline,
        lastSeenEpochMs = lastSeenEpochMs,
        status = status.name,
    )

    @JvmName("entitiesToDomainList")
    fun List<FriendEntity>.toDomainList(): List<Friend> = map { it.toDomain() }
    @JvmName("dtosToDomainList")
    fun List<FriendDto>.toDomainList(): List<Friend> = map { it.toDomain() }
    @JvmName("domainsToEntityList")
    fun List<Friend>.toEntityList(): List<FriendEntity> = map { it.toEntity() }
    @JvmName("domainsToDtoList")
    fun List<Friend>.toDtoList(): List<FriendDto> = map { it.toDto() }
}
