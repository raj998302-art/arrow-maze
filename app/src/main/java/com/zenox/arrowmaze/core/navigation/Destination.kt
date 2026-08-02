package com.zenox.arrowmaze.core.navigation

import kotlinx.serialization.Serializable

/**
 * All navigation destinations. Using a sealed interface so that route additions
 * are exhaustive-checked by the compiler at the NavHost.
 *
 * Each object is also a route constant; the NavHost matches by [route].
 */
sealed interface Destination : java.io.Serializable {

    val route: String

    @Serializable data object Splash : Destination { override val route = "splash" }

    @Serializable data object Auth : Destination { override val route = "auth" }

    @Serializable data object Home : Destination { override val route = "home" }

    // Bottom-nav roots
    @Serializable data object Shop : Destination { override val route = "shop" }
    @Serializable data object Profile : Destination { override val route = "profile" }
    @Serializable data object Leaderboard : Destination { override val route = "leaderboard" }

    // Game flows
    @Serializable data class Game(val level: Int, val isDaily: Boolean = false) : Destination {
        override val route = "game/{level}/{isDaily}"
        companion object {
            const val PATTERN = "game/{level}/{isDaily}"
            fun build(level: Int, isDaily: Boolean = false) = "game/$level/$isDaily"
        }
    }

    @Serializable data object Practice : Destination { override val route = "practice" }

    @Serializable data object DailyChallenge : Destination { override val route = "daily" }

    @Serializable data object Achievements : Destination { override val route = "achievements" }

    @Serializable data object Statistics : Destination { override val route = "statistics" }

    @Serializable data object Settings : Destination { override val route = "settings" }

    @Serializable data object Friends : Destination { override val route = "friends" }

    @Serializable data class ShopItem(val itemId: String) : Destination {
        override val route = "shop/item/{itemId}"
        companion object {
            const val PATTERN = "shop/item/{itemId}"
            fun build(itemId: String) = "shop/item/$itemId"
        }
    }
}
