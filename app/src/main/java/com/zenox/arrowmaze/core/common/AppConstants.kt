package com.zenox.arrowmaze.core.common

/**
 * App-wide constants. Centralised so that no magic strings leak across modules.
 */
object AppConstants {
    const val PACKAGE_NAME = "com.zenox.arrowmaze"

    // Deep links
    const val DEEP_LINK_SCHEME = "arrowmaze"
    const val DEEP_LINK_HOST = "open"

    // DataStore names
    const val SETTINGS_DATASTORE = "arrow_maze_settings"
    const val SESSION_DATASTORE = "arrow_maze_session"
    const val PROGRESS_DATASTORE = "arrow_maze_progress"

    // Firestore collection names
    const val FS_USERS = "users"
    const val FS_PROFILES = "profiles"
    const val FS_LEADERBOARDS = "leaderboards"
    const val FS_FRIENDS = "friends"
    const val FS_FRIEND_REQUESTS = "friend_requests"
    const val FS_ACHIEVEMENTS = "achievements"
    const val FS_DAILY_CHALLENGES = "daily_challenges"
    const val FS_STATS = "stats"

    // Remote Config keys
    const val RC_MIN_APP_VERSION = "min_app_version"
    const val RC_DAILY_REWARD_COINS = "daily_reward_coins"
    const val RC_DAILY_REWARD_XP = "daily_reward_xp"
    const val RC_INTERSTITIAL_COOLDOWN_SECONDS = "interstitial_cooldown_seconds"
    const val RC_FEATURE_FLAGS_JSON = "feature_flags_json"

    // Economy
    const val STARTING_COINS = 100
    const val STARTING_HINTS = 3
    const val STARTING_LIVES = 5
    const val MAX_LIVES = 5
    const val LIFE_REGEN_MINUTES = 30
    const val HINT_COST_COINS = 20
    const val COIN_REWARD_PER_LEVEL = 10
    const val XP_REWARD_PER_LEVEL = 50
    const val DAILY_STREAK_GRACE_DAYS = 1

    // Level progression
    const val MAX_LEVEL = Int.MAX_VALUE
    const val XP_PER_LEVEL = 1000

    // Achievements
    const val TOTAL_ACHIEVEMENTS_TARGET = 100

    // Timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val FIRESTORE_WRITE_TIMEOUT_MS = 15000L
}
