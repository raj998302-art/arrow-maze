package com.zenox.arrowmaze.core.data.repository.impl

import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.mapper.ProfileMapper.toDomain
import com.zenox.arrowmaze.core.data.mapper.ProfileMapper.toDto
import com.zenox.arrowmaze.core.data.mapper.ProfileMapper.toEntity
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.database.dao.ProfileDao
import com.zenox.arrowmaze.core.domain.model.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first [ProfileRepository]. Every write hits Room synchronously on the
 * IO dispatcher; the Firestore sync is best-effort and logged via Timber.
 *
 * Firestore sync: Phase 10
 */
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProfileRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val listStringSerializer = ListSerializer(String.serializer())

    override suspend fun getProfile(uid: String): Result<Profile> = withContext(io) {
        resultOf {
            val entity = profileDao.get(uid)
                ?: throw NoSuchElementException("Profile not found for uid=$uid")
            entity.toDomain()
        }
    }

    override fun observeProfile(uid: String): Flow<Profile?> =
        profileDao.observe(uid).map { it?.toDomain() }

    override suspend fun saveProfile(profile: Profile): Result<Unit> = withContext(io) {
        resultOf {
            profileDao.upsert(profile.toEntity())
            Timber.d("Saved profile locally: uid=%s, level=%d", profile.uid, profile.level)
            // Firestore sync: Phase 10
            syncProfileToFirestoreBestEffort(profile)
        }
    }

    override suspend fun updateEconomy(uid: String, coins: Int, hints: Int, lives: Int): Result<Unit> =
        withContext(io) {
            resultOf {
                profileDao.updateEconomy(uid, coins, hints, lives)
                Timber.d("Economy updated: uid=%s coins=%d hints=%d lives=%d", uid, coins, hints, lives)
                // Firestore sync: Phase 10
            }
        }

    override suspend fun updateProgress(uid: String, level: Int, xp: Int): Result<Unit> =
        withContext(io) {
            resultOf {
                profileDao.updateProgress(uid, level, xp)
                Timber.d("Progress updated: uid=%s level=%d xp=%d", uid, level, xp)
                // Firestore sync: Phase 10
            }
        }

    override suspend fun updateEquippedCosmetics(
        uid: String,
        themeId: String,
        arrowSkinId: String,
        trailFxId: String,
    ): Result<Unit> = withContext(io) {
        resultOf {
            profileDao.updateEquippedCosmetics(uid, themeId, arrowSkinId, trailFxId)
            Timber.d("Equipped cosmetics: uid=%s theme=%s arrow=%s trail=%s", uid, themeId, arrowSkinId, trailFxId)
            // Firestore sync: Phase 10
        }
    }

    override suspend fun addOwnedItem(uid: String, itemId: String): Result<Unit> = withContext(io) {
        resultOf {
            val current = profileDao.get(uid)
                ?: throw NoSuchElementException("Profile not found for uid=$uid")
            val owned = decodeList(current.ownedItems).toMutableSet()
            if (owned.add(itemId)) {
                profileDao.updateOwnedItems(uid, encodeList(owned.toList()))
                Timber.d("Owned item added: uid=%s item=%s", uid, itemId)
                // Firestore sync: Phase 10
            }
        }
    }

    override suspend fun addUnlockedAchievement(uid: String, achievementId: String): Result<Unit> =
        withContext(io) {
            resultOf {
                val current = profileDao.get(uid)
                    ?: throw NoSuchElementException("Profile not found for uid=$uid")
                val unlocked = decodeList(current.unlockedAchievements).toMutableSet()
                if (unlocked.add(achievementId)) {
                    val updated = current.copy(unlockedAchievements = encodeList(unlocked.toList()))
                    profileDao.upsert(updated)
                    Timber.d("Achievement unlocked on profile: uid=%s id=%s", uid, achievementId)
                    // Firestore sync: Phase 10
                }
            }
        }

    override suspend fun mergeGuestIntoAccount(guestProfile: Profile, accountUid: String): Result<Unit> =
        withContext(io) {
            resultOf {
                val accountEntity = profileDao.get(accountUid)
                val merged = if (accountEntity == null) {
                    // Promote the guest profile to a real account: copy all
                    // fields except uid + isGuest + email (which the caller
                    // should have set on the supplied account uid).
                    guestProfile.copy(uid = accountUid, isGuest = false)
                } else {
                    val account = accountEntity.toDomain()
                    account.copy(
                        coins = account.coins + guestProfile.coins,
                        hints = account.hints + guestProfile.hints,
                        level = maxOf(account.level, guestProfile.level),
                        xp = account.xp + guestProfile.xp,
                        highestLevel = maxOf(account.highestLevel, guestProfile.highestLevel),
                        ownedItems = (account.ownedItems + guestProfile.ownedItems).distinct(),
                        unlockedAchievements = (account.unlockedAchievements + guestProfile.unlockedAchievements).distinct(),
                        gamesPlayed = account.gamesPlayed + guestProfile.gamesPlayed,
                        gamesWon = account.gamesWon + guestProfile.gamesWon,
                        bestStreak = maxOf(account.bestStreak, guestProfile.bestStreak),
                    )
                }
                profileDao.upsert(merged.toEntity())
                // Best-effort: delete the guest row.
                runCatching { profileDao.delete(guestProfile.uid) }
                Timber.i("Merged guest=%s into account=%s", guestProfile.uid, accountUid)
                // Firestore sync: Phase 10
            }
        }

    // ---------- helpers ----------

    private fun encodeList(values: List<String>): String =
        json.encodeToString(listStringSerializer, values)

    private fun decodeList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(listStringSerializer, value) }
            .getOrElse { emptyList() }
    }

    /** Best-effort Firestore sync stub. Real implementation lands in Phase 10. */
    private fun syncProfileToFirestoreBestEffort(profile: Profile) {
        // Serialise the DTO so the Phase 10 wiring can hand it straight to
        // Firestore's `set(profileRef, profileDto.toMap())`. For now we just
        // touch the DTO so dead-code elimination doesn't strip the mapper.
        val dto = profile.toDto()
        Timber.v("Prepared Firestore sync payload for uid=%s", dto.uid)
        // Intentionally no Firestore call yet — see Phase 10.
    }
}
