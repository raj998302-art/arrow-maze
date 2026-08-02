package com.zenox.arrowmaze.features.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeTheme
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.engine.PuzzleGenerator
import com.zenox.arrowmaze.core.domain.model.ArrowDirection
import com.zenox.arrowmaze.core.domain.model.Board
import com.zenox.arrowmaze.core.domain.model.Cell
import com.zenox.arrowmaze.core.domain.model.GameSession
import com.zenox.arrowmaze.core.domain.model.LevelConfig
import com.zenox.arrowmaze.core.domain.model.LevelProgression
import com.zenox.arrowmaze.core.domain.model.Position
import com.zenox.arrowmaze.features.game.components.GameBoardCanvas
import com.zenox.arrowmaze.features.game.components.GameHud
import com.zenox.arrowmaze.features.game.components.WinOverlay

/**
 * Preview composables for the Game screen. Each preview mounts a fully
 * realised [GameUiState] snapshot so the Compose preview pane renders the
 * exact UI the player sees in production — no mocking of the ViewModel
 * required.
 *
 * All previews are wrapped in [ArrowMazeTheme] so colours / typography /
 * shapes match production rendering.
 */

@Preview(showBackground = true, name = "Game — Playing 4×4 (Level 1)", heightDp = 720, widthDp = 360)
@Composable
private fun GamePlayingPreview() {
    val level = 1
    val config = LevelProgression.configFor(level)
    val board = PuzzleGenerator.generate(config, seed = 42L)
    val session = GameSession(
        board = board,
        config = config,
        movesPlayed = 3,
        hintsUsed = 0,
        elapsedMs = 12_000L,
        isWon = false,
        isLost = false,
        path = listOf(board.start),
    )
    val state = GameUiState.Playing(
        session = session,
        path = session.path,
        lastRotatedCell = null,
        coins = 100,
        hints = 3,
        lives = 5,
        config = config,
    )
    ArrowMazeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GameScreenPreviewContent(state = state)
        }
    }
}

@Preview(showBackground = true, name = "Game — Playing 9×9 (Level 500)", heightDp = 720, widthDp = 360)
@Composable
private fun GamePlayingLegendPreview() {
    val level = 500
    val config = LevelProgression.configFor(level)
    val board = PuzzleGenerator.generate(config, seed = 1234L)
    val session = GameSession(
        board = board,
        config = config,
        movesPlayed = 18,
        hintsUsed = 1,
        elapsedMs = 95_000L,
        isWon = false,
        isLost = false,
        path = listOf(board.start),
    )
    val state = GameUiState.Playing(
        session = session,
        path = session.path,
        lastRotatedCell = null,
        coins = 250,
        hints = 1,
        lives = 3,
        config = config,
    )
    ArrowMazeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GameScreenPreviewContent(state = state)
        }
    }
}

@Preview(showBackground = true, name = "Game — Won 4×4", heightDp = 720, widthDp = 360)
@Composable
private fun GameWonPreview() {
    val level = 1
    val config = LevelProgression.configFor(level)
    val board = PuzzleGenerator.generate(config, seed = 42L)
    val path = solvePathForPreview(board) ?: listOf(board.start, board.goal)
    val session = GameSession(
        board = board,
        config = config,
        movesPlayed = 6,
        hintsUsed = 0,
        elapsedMs = 21_000L,
        isWon = true,
        isLost = false,
        path = path,
    )
    val state = GameUiState.Won(
        session = session,
        path = path,
        coinsEarned = 12,
        xpEarned = 55,
        timeMs = 21_000L,
        moves = 6,
    )
    ArrowMazeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                GameScreenPreviewContent(state = state.toPlayingForDisplay())
                WinOverlay(
                    state = state,
                    onContinue = {},
                    onReplay = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Game — Board only 5×5", heightDp = 480, widthDp = 360)
@Composable
private fun GameBoardOnlyPreview() {
    val level = 25
    val config = LevelProgression.configFor(level)
    val board = PuzzleGenerator.generate(config, seed = 7L)
    ArrowMazeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SpacingTokens.md),
                contentAlignment = Alignment.Center,
            ) {
                GameBoardCanvas(
                    board = board,
                    path = emptyList(),
                    lastRotatedCell = null,
                    onCellTap = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// -------------------------------------------------------------------
// Preview-only helpers
// -------------------------------------------------------------------

/**
 * Lightweight in-process replica of the production GameScreen layout. Takes
 * a state directly so the preview pane doesn't need a Hilt-injected
 * ViewModel.
 */
@Composable
private fun GameScreenPreviewContent(state: GameUiState.Playing) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameHud(
            level = state.config.level,
            moves = state.session.movesPlayed,
            timeMs = state.session.elapsedMs,
            coins = state.coins,
            hints = state.hints,
            lives = state.lives,
            onHint = {},
            onRestart = {},
            onBack = {},
            onSettings = {},
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.md)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            GameBoardCanvas(
                board = state.session.board,
                path = state.path,
                lastRotatedCell = state.lastRotatedCell,
                onCellTap = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Best-effort path finder for the preview — walks the arrows from the start
 * until we either reach the goal or hit a non-arrow cell. Used to draw a
 * realistic solved trail in the Won preview. Falls back to `null` if no
 * path exists (which lets the preview use a 2-cell start→goal fallback).
 */
private fun solvePathForPreview(board: Board): List<Position>? {
    val path = mutableListOf<Position>()
    var current = board.start
    val visited = mutableSetOf<Position>()
    while (current.isWithin(board.size)) {
        if (current in visited) return null
        visited.add(current)
        path.add(current)
        if (current == board.goal) return path
        val cell = board.cellAtOrNull(current) ?: return null
        if (cell !is Cell.ArrowCell) return null
        val (dr, dc) = cell.direction.delta()
        current = current.plus(dr, dc)
    }
    return null
}

/** Preview-only conversion mirroring [GameUiState.Won.toPlayingForDisplay]. */
private fun GameUiState.Won.toPlayingForDisplay(): GameUiState.Playing =
    GameUiState.Playing(
        session = session,
        path = path,
        lastRotatedCell = null,
        coins = 0,
        hints = 0,
        lives = 0,
        config = session.config,
    )

/** Reserved for future use — exported so downstream preview files can
 *  mirror the preview palette without duplicating it. */
@Suppress("unused")
private fun previewPalette(): List<ArrowDirection> = ArrowDirection.entries
