package com.zenox.arrowmaze.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Bottom-navigation roots.
 */
private data class BottomTab(
    val destination: Destination,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Destination.Home, "Home", Icons.Filled.Home),
    BottomTab(Destination.Shop, "Shop", Icons.Filled.Storefront),
    BottomTab(Destination.Leaderboard, "Leaders", Icons.Filled.Leaderboard),
    BottomTab(Destination.Profile, "Profile", Icons.Filled.Person),
)

/**
 * Root navigation host. Phase 2 wires placeholders for every destination so
 * that the nav graph compiles and deep links resolve. Phase 6+ replaces each
 * placeholder with the real feature screen.
 */
@Composable
fun ArrowMazeNavHost(
    startDestination: Destination = Destination.Splash,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf(
        Destination.Home.route,
        Destination.Shop.route,
        Destination.Leaderboard.route,
        Destination.Profile.route,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(Destination.Splash.route) {
                com.zenox.arrowmaze.features.home.SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToAuth = {
                        navController.navigate(Destination.Auth.route) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destination.Auth.route) {
                com.zenox.arrowmaze.features.authentication.AuthScreen(
                    onAuthenticated = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Auth.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destination.Home.route) {
                com.zenox.arrowmaze.features.home.HomeScreen(
                    onPlay = { level -> navController.navigate(Destination.Game.build(level)) },
                    onDailyChallenge = { navController.navigate(Destination.DailyChallenge.route) },
                    onPractice = { navController.navigate(Destination.Game.build(level = 1)) },
                    onShop = { navController.navigate(Destination.Shop.route) },
                    onAchievements = { navController.navigate(Destination.Achievements.route) },
                    onLeaderboard = { navController.navigate(Destination.Leaderboard.route) },
                    onProfile = { navController.navigate(Destination.Profile.route) },
                    onSettings = { navController.navigate(Destination.Settings.route) },
                )
            }
            composable(Destination.Shop.route) {
                com.zenox.arrowmaze.features.shop.ShopScreen(
                    onNavigateToItem = { itemId -> navController.navigate(Destination.ShopItem.build(itemId)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Profile.route) {
                com.zenox.arrowmaze.features.profile.ProfileScreen(
                    onNavigateToSettings = { navController.navigate(Destination.Settings.route) },
                    onNavigateToAchievements = { navController.navigate(Destination.Achievements.route) },
                    onNavigateToStatistics = { navController.navigate(Destination.Statistics.route) },
                    onNavigateToFriends = { navController.navigate(Destination.Friends.route) },
                    onSignedOut = {
                        navController.navigate(Destination.Auth.route) {
                            popUpTo(Destination.Home.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destination.Leaderboard.route) {
                com.zenox.arrowmaze.features.leaderboard.LeaderboardScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.Game.PATTERN,
                arguments = listOf(
                    navArgument("level") { type = NavType.IntType },
                    navArgument("isDaily") { type = NavType.BoolType },
                ),
            ) { entry ->
                val level = entry.arguments?.getInt("level") ?: 1
                val isDaily = entry.arguments?.getBoolean("isDaily") ?: false
                // GameScreen reads level + isDaily from its own SavedStateHandle
                // (Hilt's NavArgs binding), so we don't need to thread them
                // through explicitly here. They're surfaced for logging only.
                @Suppress("UNUSED_VARIABLE")
                val levelArg = level
                @Suppress("UNUSED_VARIABLE")
                val isDailyArg = isDaily
                com.zenox.arrowmaze.features.game.GameScreen(
                    onBack = { navController.popBackStack() },
                    onNextLevel = { nextLevel ->
                        navController.navigate(Destination.Game.build(nextLevel)) {
                            popUpTo(Destination.Game.PATTERN) { inclusive = true }
                        }
                    },
                    onSettings = { navController.navigate(Destination.Settings.route) },
                )
            }
            composable(Destination.Practice.route) {
                // Practice mode reuses the game screen with a practice flag (Phase 10 will
                // add a dedicated practice setup screen; for now jump straight to an easy board)
                navController.navigate(Destination.Game.build(level = 1))
            }
            composable(Destination.DailyChallenge.route) {
                com.zenox.arrowmaze.features.dailychallenge.DailyChallengeScreen(
                    onStartChallenge = {
                        navController.navigate(Destination.Game.build(level = 1, isDaily = true))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Achievements.route) {
                com.zenox.arrowmaze.features.achievements.AchievementsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Statistics.route) {
                com.zenox.arrowmaze.features.statistics.StatisticsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Settings.route) {
                com.zenox.arrowmaze.features.settings.SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSignedOut = {
                        navController.navigate(Destination.Auth.route) {
                            popUpTo(Destination.Home.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destination.Friends.route) {
                com.zenox.arrowmaze.features.friends.FriendsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.ShopItem.PATTERN,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
            ) {
                com.zenox.arrowmaze.features.shop.ShopItemDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPurchased = { navController.popBackStack() },
                )
            }
        }
    }
}

