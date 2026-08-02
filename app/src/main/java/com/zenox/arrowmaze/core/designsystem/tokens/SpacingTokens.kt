package com.zenox.arrowmaze.core.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for Arrow Maze.
 *
 * Single source of truth for all paddings / gaps so the layout rhythm
 * (4 / 8 / 12 / 16 / 24 / 32 / 48) stays consistent across feature
 * screens. Components should consume these instead of inlining magic dp
 * values.
 */
object SpacingTokens {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}
