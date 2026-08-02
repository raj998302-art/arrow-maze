package com.zenox.arrowmaze.core.di

import com.zenox.arrowmaze.core.data.repository.AchievementRepository
import com.zenox.arrowmaze.core.data.repository.DailyChallengeRepository
import com.zenox.arrowmaze.core.data.repository.FriendRepository
import com.zenox.arrowmaze.core.data.repository.LeaderboardRepository
import com.zenox.arrowmaze.core.data.repository.LevelProgressRepository
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.data.repository.SettingsRepository
import com.zenox.arrowmaze.core.data.repository.ShopRepository
import com.zenox.arrowmaze.core.data.repository.StatsRepository
import com.zenox.arrowmaze.core.data.repository.impl.AchievementRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.DailyChallengeRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.FriendRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.LeaderboardRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.LevelProgressRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.ProfileRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.SessionRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.SettingsRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.ShopRepositoryImpl
import com.zenox.arrowmaze.core.data.repository.impl.StatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every repository interface to its concrete implementation. Uses
 * `abstract class` + `@Binds` so Hilt generates the most efficient wiring
 * (no runtime reflection).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindShopRepository(impl: ShopRepositoryImpl): ShopRepository

    @Binds
    @Singleton
    abstract fun bindLevelProgressRepository(impl: LevelProgressRepositoryImpl): LevelProgressRepository

    @Binds
    @Singleton
    abstract fun bindDailyChallengeRepository(impl: DailyChallengeRepositoryImpl): DailyChallengeRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(impl: LeaderboardRepositoryImpl): LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
