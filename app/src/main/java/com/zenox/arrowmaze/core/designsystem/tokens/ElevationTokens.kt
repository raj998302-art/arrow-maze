package com.zenox.arrowmaze.core.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * Elevation tokens for Arrow Maze.
 *
 * Five discrete elevation levels matching Material 3 elevation guidance.
 * Used by [ArrowMazeCard][com.zenox.arrowmaze.core.designsystem.components.ArrowMazeCard],
 * dialogs and floating HUD elements so shadow depth stays consistent
 * across all feature screens.
 */
object ElevationTokens {
    val Level0 = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
}
