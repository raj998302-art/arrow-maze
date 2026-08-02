package com.zenox.arrowmaze.core.di

import android.content.Context
import com.zenox.arrowmaze.core.database.ArrowMazeDatabase
import com.zenox.arrowmaze.core.database.dao.AchievementDao
import com.zenox.arrowmaze.core.database.dao.DailyChallengeDao
import com.zenox.arrowmaze.core.database.dao.FriendDao
import com.zenox.arrowmaze.core.database.dao.FriendRequestDao
import com.zenox.arrowmaze.core.database.dao.LevelProgressDao
import com.zenox.arrowmaze.core.database.dao.OwnedItemDao
import com.zenox.arrowmaze.core.database.dao.ProfileDao
import com.zenox.arrowmaze.core.database.dao.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the Room database + all 8 DAOs. The database itself is a
 * singleton; each DAO is provided as a thin pass-through so that repositories
 * can inject the DAO directly without depending on the whole database.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideArrowMazeDatabase(
        @ApplicationContext context: Context,
    ): ArrowMazeDatabase = ArrowMazeDatabase.getInstance(context)

    @Provides
    fun provideProfileDao(db: ArrowMazeDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideStatsDao(db: ArrowMazeDatabase): StatsDao = db.statsDao()

    @Provides
    fun provideAchievementDao(db: ArrowMazeDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideOwnedItemDao(db: ArrowMazeDatabase): OwnedItemDao = db.ownedItemDao()

    @Provides
    fun provideLevelProgressDao(db: ArrowMazeDatabase): LevelProgressDao = db.levelProgressDao()

    @Provides
    fun provideDailyChallengeDao(db: ArrowMazeDatabase): DailyChallengeDao = db.dailyChallengeDao()

    @Provides
    fun provideFriendDao(db: ArrowMazeDatabase): FriendDao = db.friendDao()

    @Provides
    fun provideFriendRequestDao(db: ArrowMazeDatabase): FriendRequestDao = db.friendRequestDao()
}
