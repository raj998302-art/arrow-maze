package com.zenox.arrowmaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeTheme
import com.zenox.arrowmaze.core.navigation.ArrowMazeNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the entire Jetpack Compose UI.
 *
 * - Installs the Android 12+ SplashScreen API (theme `Theme.ArrowMaze.Splash`)
 * - Enables edge-to-edge
 * - Mounts [ArrowMazeTheme] + [ArrowMazeNavHost]
 *
 * Phase 8 wires the [MainViewModel]'s `currentTheme` / `darkMode` /
 * `highContrast` flows into [ArrowMazeTheme] so the whole app re-themes
 * when the user picks a cosmetic theme in the Shop.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash on screen until the first frame is ready, then fade out.
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        setContent {
            val theme by mainViewModel.currentTheme.collectAsStateWithLifecycle()
            val darkMode by mainViewModel.darkMode.collectAsStateWithLifecycle()
            val highContrast by mainViewModel.highContrast.collectAsStateWithLifecycle()

            ArrowMazeTheme(
                darkMode = darkMode,
                themeId = theme.id,
                highContrast = highContrast,
                gameTheme = theme,
            ) {
                ArrowMazeNavHost()
            }
        }
        keepSplash = false
    }
}
