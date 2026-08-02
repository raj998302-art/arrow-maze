package com.zenox.arrowmaze.features.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeDialog
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.game.components.GameBoardCanvas
import com.zenox.arrowmaze.features.game.components.GameHud
import com.zenox.arrowmaze.features.game.components.WinOverlay
import com.zenox.arrowmaze.core.designsystem.components.LoseDialog

/**
 * Root composable for the Game screen.
 *
 * Mounts the [GameViewModel]'s state into one of five sub-views:
 *
 *  - [GameUiState.Loading] → [LoadingState]
 *  - [GameUiState.Playing] → [GameHud] + [GameBoardCanvas]
 *  - [GameUiState.Won]     → [GameHud] + [GameBoardCanvas] (dimmed) +
 *                              [WinOverlay]
 *  - [GameUiState.Lost]    → [GameHud] + [GameBoardCanvas] +
 *                              [LoseDialog]
 *  - [GameUiState.Error]   → [ErrorState] with a Retry button wired to
 *                              [GameViewModel.onRestart]
 *
 * The system back button is intercepted via [BackHandler] while a game is
 * in progress — the player is asked to confirm before quitting.
 *
 * @param viewModel   Hilt-injected [GameViewModel].
 * @param onBack      Fired when the player taps the HUD back button or
 *                     confirms the quit-current-game dialog.
 * @param onNextLevel Fired with the next level number when the player taps
 *                     "Continue" on the win overlay.
 * @param onSettings  Fired when the player taps the HUD settings icon.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuitConfirmation by remember { mutableStateOf(false) }

    // Confirm-before-quit only while a game is actively in progress.
    val isPlaying = state is GameUiState.Playing
    BackHandler(enabled = isPlaying) {
        showQuitConfirmation = true
    }

    if (showQuitConfirmation) {
        ArrowMazeDialog(
            title = "Quit current game?",
            message = "Progress on this level will be lost.",
            confirmText = "Quit",
            onConfirm = {
                showQuitConfirmation = false
                onBack()
            },
            dismissText = "Keep playing",
            onDismiss = { showQuitConfirmation = false },
        )
    }

    when (val s = state) {
        is GameUiState.Loading -> {
            LoadingState(message = "Loading Level ${s.level}…")
        }

        is GameUiState.Playing -> {
            PlayingContent(
                state = s,
                onCellTap = viewModel::onCellTapped,
                onHint = viewModel::onHintUsed,
                onRestart = viewModel::onRestart,
                onBack = onBack,
                onSettings = onSettings,
            )
        }

        is GameUiState.Won -> {
            // Keep the board visible behind the overlay so the player sees
            // their solved path.
            Box(modifier = Modifier.fillMaxSize()) {
                PlayingContent(
                    state = s.toPlayingForDisplay(),
                    onCellTap = { /* no-op after win */ },
                    onHint = { /* no-op */ },
                    onRestart = viewModel::onRestart,
                    onBack = onBack,
                    onSettings = onSettings,
                )
                WinOverlay(
                    state = s,
                    onContinue = {
                        viewModel.onContinueAfterWin()
                        onNextLevel(s.session.config.level + 1)
                    },
                    onReplay = {
                        viewModel.onContinueAfterWin()
                        viewModel.onRestart()
                    },
                )
            }
        }

        is GameUiState.Lost -> {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayingContent(
                    state = s.toPlayingForDisplay(),
                    onCellTap = { /* no-op after loss */ },
                    onHint = { /* no-op */ },
                    onRestart = viewModel::onRestart,
                    onBack = onBack,
                    onSettings = onSettings,
                )
                LoseDialog(
                    visible = true,
                    onRetry = { viewModel.onRestart() },
                    onUseHint = {
                        // Hints are no longer usable after a loss; route the
                        // player back to the restart flow instead.
                        viewModel.onRestart()
                    },
                    canUseHint = false,
                )
            }
        }

        is GameUiState.Error -> {
            ErrorState(
                message = s.message,
                onRetry = { viewModel.onRestart() },
            )
        }
    }
}

/**
 * Shared "HUD on top, board below" layout used by both [GameUiState.Playing]
 * and as the dimmed background under [GameUiState.Won] / [GameUiState.Lost].
 */
@Composable
private fun PlayingContent(
    state: GameUiState.Playing,
    onCellTap: (com.zenox.arrowmaze.core.domain.model.Position) -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GameHud(
            level = state.config.level,
            moves = state.session.movesPlayed,
            timeMs = state.session.elapsedMs,
            coins = state.coins,
            hints = state.hints,
            lives = state.lives,
            onHint = onHint,
            onRestart = onRestart,
            onBack = onBack,
            onSettings = onSettings,
        )

        Spacer(Modifier.height(SpacingTokens.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.md)
                .weight(1f, fill = true),
            contentAlignment = Alignment.Center,
        ) {
            GameBoardCanvas(
                board = state.session.board,
                path = state.path,
                lastRotatedCell = state.lastRotatedCell,
                onCellTap = onCellTap,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(SpacingTokens.md))

        // Footer hint: lets the player know taps rotate arrows clockwise.
        Text(
            text = stringResource(R.string.game_hint) + ": tap an arrow to rotate it 90°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingTokens.sm),
        )
    }
}

/** Helper: convert [GameUiState.Won] back to [GameUiState.Playing] for the
 *  dimmed board background behind the win overlay. */
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

/** Helper: convert [GameUiState.Lost] back to [GameUiState.Playing] for the
 *  dimmed board background behind the lose dialog. */
private fun GameUiState.Lost.toPlayingForDisplay(): GameUiState.Playing =
    GameUiState.Playing(
        session = session,
        path = session.path,
        lastRotatedCell = null,
        coins = coins,
        hints = hints,
        lives = 0,
        config = session.config,
    )

/**
 * Reserved for future skinning hooks — kept exported so downstream feature
 * modules can extend the Game screen without modifying it directly.
 */
@Suppress("unused")
private fun gameScreenFooterText(): String = "Tap arrows to rotate. Reach the goal."
