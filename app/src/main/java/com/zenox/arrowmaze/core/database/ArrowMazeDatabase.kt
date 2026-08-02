package com.zenox.arrowmaze.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zenox.arrowmaze.core.database.dao.AchievementDao
import com.zenox.arrowmaze.core.database.dao.DailyChallengeDao
import com.zenox.arrowmaze.core.database.dao.FriendDao
import com.zenox.arrowmaze.core.database.dao.FriendRequestDao
import com.zenox.arrowmaze.core.database.dao.LevelProgressDao
import com.zenox.arrowmaze.core.database.dao.OwnedItemDao
import com.zenox.arrowmaze.core.database.dao.ProfileDao
import com.zenox.arrowmaze.core.database.dao.StatsDao
import com.zenox.arrowmaze.core.database.entity.AchievementProgressEntity
import com.zenox.arrowmaze.core.database.entity.DailyChallengeEntity
import com.zenox.arrowmaze.core.database.entity.FriendEntity
import com.zenox.arrowmaze.core.database.entity.FriendRequestEntity
import com.zenox.arrowmaze.core.database.entity.LevelProgressEntity
import com.zenox.arrowmaze.core.database.entity.OwnedItemEntity
import com.zenox.arrowmaze.core.database.entity.ProfileEntity
import com.zenox.arrowmaze.core.database.entity.StatsEntity

/**
 * Root Room database. Holds all local-first persistence for Arrow Maze:
 * profiles, stats, achievement progress, owned items, level progress, daily
 * challenges, friends, and friend requests.
 *
 * `exportSchema = false` keeps the build self-contained (no schema-output dir
 * needs to be configured). Switch on if a CI schema check is added later.
 *
 * `fallbackToDestructiveMigration` is acceptable for v1 since the app is
 * pre-launch; replace with explicit migrations before public release.
 */
@Database(
    entities = [
        ProfileEntity::class,
        StatsEntity::class,
        AchievementProgressEntity::class,
        OwnedItemEntity::class,
        LevelProgressEntity::class,
        DailyChallengeEntity::class,
        FriendEntity::class,
        FriendRequestEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ArrowMazeDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun statsDao(): StatsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun ownedItemDao(): OwnedItemDao
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun friendDao(): FriendDao
    abstract fun friendRequestDao(): FriendRequestDao

    companion object {
        private const val DATABASE_NAME = "arrow_maze.db"

        @Volatile
        private var INSTANCE: ArrowMazeDatabase? = null

        /**
         * Returns the singleton database. Falls back to destructive migration
         * so a schema bump during development can't brick the app.
         */
        fun getInstance(context: Context): ArrowMazeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ArrowMazeDatabase::class.java,
                    DATABASE_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
