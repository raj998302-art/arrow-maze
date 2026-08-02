package com.zenox.arrowmaze.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeDialog
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.CoinCounter
import com.zenox.arrowmaze.core.designsystem.components.CoinCounterSize
import com.zenox.arrowmaze.core.designsystem.components.EmptyState
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.GameTopBar
import com.zenox.arrowmaze.core.designsystem.components.GradientBackground
import com.zenox.arrowmaze.core.designsystem.components.HudPill
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.components.SectionHeader
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeTheme

/**
 * Preview-only file. All previews wrap content in [ArrowMazeTheme] so the
 * brand colour scheme + typography are applied. Does not affect the
 * production build (R8 strips these out via the `tooling-preview` debug
 * source set).
 */

@Preview(showBackground = true, name = "ArrowMazeButton — all variants")
@Composable
private fun ArrowMazeButtonPreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ArrowMazeButton(text = "Primary", onClick = {}, style = ButtonStyle.Primary)
                ArrowMazeButton(text = "Secondary", onClick = {}, style = ButtonStyle.Secondary)
                ArrowMazeButton(text = "Tonal", onClick = {}, style = ButtonStyle.Tonal)
                ArrowMazeButton(text = "Outline", onClick = {}, style = ButtonStyle.Outline)
                ArrowMazeButton(text = "Glass", onClick = {}, style = ButtonStyle.Glass)
                ArrowMazeButton(text = "Loading", onClick = {}, isLoading = true)
                ArrowMazeButton(
                    text = "With Icons",
                    onClick = {},
                    style = ButtonStyle.Primary,
                    leadingIcon = ArrowMazeIcons.Coin,
                    trailingIcon = ArrowMazeIcons.ArrowRight,
                )
                ArrowMazeButton(
                    text = "Disabled",
                    onClick = {},
                    enabled = false,
                    style = ButtonStyle.Primary,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "HudPill")
@Composable
private fun HudPillPreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudPill(icon = ArrowMazeIcons.Coin, count = 42, contentDescription = "Coins")
                HudPill(icon = ArrowMazeIcons.Hint, count = 3, contentDescription = "Hints")
                HudPill(icon = ArrowMazeIcons.Life, count = 5, contentDescription = "Lives")
            }
        }
    }
}

@Preview(showBackground = true, name = "GradientBackground", heightDp = 320, widthDp = 320)
@Composable
private fun GradientBackgroundPreview() {
    ArrowMazeTheme {
        GradientBackground(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Brand gradient backdrop",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "SectionHeader")
@Composable
private fun SectionHeaderPreview() {
    ArrowMazeTheme {
        Surface {
            Column {
                SectionHeader(
                    title = "Daily Challenge",
                    subtitle = "Solve today's puzzle for 50 coins",
                    leadingIcon = ArrowMazeIcons.Sparkle,
                )
                SectionHeader(title = "Achievements")
            }
        }
    }
}

@Preview(showBackground = true, name = "LoadingState")
@Composable
private fun LoadingStatePreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.size(280.dp, 200.dp)) {
            LoadingState(message = "Loading level…")
        }
    }
}

@Preview(showBackground = true, name = "ErrorState")
@Composable
private fun ErrorStatePreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.size(320.dp, 280.dp)) {
            ErrorState(
                message = "Couldn't load the leaderboard. Check your connection.",
                onRetry = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "EmptyState")
@Composable
private fun EmptyStatePreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.size(320.dp, 320.dp)) {
            EmptyState(
                icon = ArrowMazeIcons.Trophy,
                title = "No achievements yet",
                subtitle = "Play your first level to start unlocking rewards.",
                actionText = "Play now",
                onAction = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "ArrowMazeDialog")
@Composable
private fun ArrowMazeDialogPreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.size(320.dp, 240.dp)) {
            ArrowMazeDialog(
                title = "Sign out?",
                message = "You'll need to sign in again to sync your progress.",
                confirmText = "Sign out",
                onConfirm = {},
                dismissText = "Cancel",
                onDismiss = {},
                icon = ArrowMazeIcons.Lock,
            )
        }
    }
}

@Preview(showBackground = true, name = "CoinCounter")
@Composable
private fun CoinCounterPreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CoinCounter(count = 99, size = CoinCounterSize.Small)
                CoinCounter(count = 1250, size = CoinCounterSize.Medium)
                CoinCounter(count = 999999, size = CoinCounterSize.Large)
            }
        }
    }
}

@Preview(showBackground = true, name = "GameTopBar", heightDp = 80)
@Composable
private fun GameTopBarPreview() {
    ArrowMazeTheme {
        Surface {
            GameTopBar(
                title = "Level 12",
                subtitle = "Hard",
                onBack = {},
                onSettings = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "ArrowMazeIcons grid", heightDp = 200)
@Composable
private fun ArrowMazeIconsPreview() {
    ArrowMazeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IconsRow(
                    listOf(
                        ArrowMazeIcons.ArrowUp,
                        ArrowMazeIcons.ArrowDown,
                        ArrowMazeIcons.ArrowLeft,
                        ArrowMazeIcons.ArrowRight,
                    ),
                )
                IconsRow(
                    listOf(
                        ArrowMazeIcons.Coin,
                        ArrowMazeIcons.Hint,
                        ArrowMazeIcons.Life,
                        ArrowMazeIcons.Trophy,
                    ),
                )
                IconsRow(
                    listOf(
                        ArrowMazeIcons.Target,
                        ArrowMazeIcons.Sparkle,
                        ArrowMazeIcons.Lock,
                    ),
                )
            }
        }
    }
}

@Composable
private fun IconsRow(icons: List<ImageVector>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        icons.forEach { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
