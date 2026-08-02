package com.zenox.arrowmaze.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.domain.model.Profile
import com.zenox.arrowmaze.features.profile.components.LevelProgressBar
import com.zenox.arrowmaze.features.profile.components.StatCard
import com.zenox.arrowmaze.features.profile.components.formatDuration
import com.zenox.arrowmaze.features.profile.components.toPercentString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Root composable for the Profile screen.
 *
 * Mounts the [ProfileViewModel]'s state into one of three sub-views:
 *
 *  - [ProfileUiState.Loading] → [LoadingState]
 *  - [ProfileUiState.Error]   → [ErrorState]
 *  - [ProfileUiState.Success] → [ProfileContent] (avatar, stats grid,
 *    cosmetics, action buttons, sign-out / delete-account surface).
 *
 * Navigation callbacks:
 *  - [onNavigateToSettings]     — gear icon in the top bar.
 *  - [onNavigateToAchievements] — "Achievements" action button.
 *  - [onNavigateToStatistics]   — "Statistics" action button.
 *  - [onNavigateToFriends]      — "Friends" action button.
 *  - [onSignedOut]              — fired after sign-out OR account deletion.
 *
 * @param viewModel Hilt-injected [ProfileViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Route one-shot nav events.
    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is ProfileNavEvent.SignedOut,
                is ProfileNavEvent.AccountDeleted -> onSignedOut()
                is ProfileNavEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    ArrowMazeIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        onClick = onNavigateToSettings,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> LoadingState(message = "Loading profile…")
                is ProfileUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { /* state is auto-driven by the uid flow */ },
                )
                is ProfileUiState.Success -> ProfileContent(
                    state = state,
                    viewModel = viewModel,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onNavigateToStatistics = onNavigateToStatistics,
                    onNavigateToFriends = onNavigateToFriends,
                )
            }
        }
    }
}

/**
 * Inner content for the [ProfileUiState.Success] state. Renders the avatar
 * header, the stats grid, the equipped cosmetics row, and the action
 * buttons (including the sign-out / delete-account surface).
 */
@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    viewModel: ProfileViewModel,
    onNavigateToAchievements: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFriends: () -> Unit,
) {
    val profile = state.profile
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.lg),
    ) {
        ProfileHeader(profile = profile)

        // Guest CTA banner.
        if (profile.isGuest) {
            GuestBanner()
        }

        // Level + XP progress bar.
        LevelProgressBar(profile = profile)

        // Stats grid (3 columns).
        StatsGrid(profile = profile)

        // Equipped cosmetics row.
        CosmeticsRow(profile = profile)

        // Action buttons row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            ActionButton(
                label = "Achievements",
                icon = Icons.Rounded.EmojiEvents,
                onClick = onNavigateToAchievements,
            )
            ActionButton(
                label = "Statistics",
                icon = Icons.Rounded.Insights,
                onClick = onNavigateToStatistics,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            ActionButton(
                label = "Friends",
                icon = Icons.Rounded.Group,
                onClick = onNavigateToFriends,
            )
            ActionButton(
                label = "Edit Profile",
                icon = Icons.Rounded.Edit,
                onClick = { showEditDialog = true },
            )
        }

        Spacer(Modifier.height(SpacingTokens.sm))

        // Sign-out + delete-account surface.
        ArrowMazeButton(
            text = stringResource(R.string.auth_logout),
            onClick = viewModel::signOut,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Tonal,
            leadingIcon = Icons.AutoMirrored.Rounded.Logout,
        )
        Spacer(Modifier.height(SpacingTokens.xs))
        ArrowMazeButton(
            text = stringResource(R.string.auth_delete_account),
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Outline,
            leadingIcon = Icons.Rounded.Delete,
        )

        Spacer(Modifier.height(SpacingTokens.xxl))
    }

    if (showEditDialog) {
        EditProfileDialog(
            initialDisplayName = profile.displayName,
            initialPlayerName = profile.playerName,
            initialCountry = profile.country,
            initialAvatarUrl = profile.avatarUrl,
            onSave = { displayName, playerName, country, avatarUrl ->
                viewModel.updateDisplayName(displayName)
                viewModel.updatePlayerName(playerName)
                viewModel.updateCountry(country)
                viewModel.updateAvatar(avatarUrl)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = {
                Text(
                    "This permanently deletes your account and all associated " +
                        "progress. This action cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

/**
 * Avatar + display name + player id + country + join date header.
 */
@Composable
private fun ProfileHeader(profile: Profile) {
    val cs = MaterialTheme.colorScheme
    val initials = remember(profile.displayName) {
        profile.displayName.split(' ').take(2).joinToString("") { word ->
            word.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        }.ifEmpty { "?" }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.lg),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (!profile.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "@${profile.playerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ID: ${profile.uid.take(8)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.width(SpacingTokens.md))
                Text(
                    text = "${flagForCountry(profile.country)}  ${profile.country}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            Text(
                text = "Joined ${formatJoinDate(profile.joinDateEpochMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/** "Playing as guest" banner shown when [Profile.isGuest] is true. */
@Composable
private fun GuestBanner() {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(cs.tertiaryContainer)
            .padding(SpacingTokens.md),
    ) {
        Text(
            text = "Playing as guest — Sign in to save progress",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onTertiaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Three-column stats grid rendered via `.chunked(3)` rows. */
@Composable
private fun StatsGrid(profile: Profile) {
    val stats = listOf(
        StatItem(ArrowMazeIcons.Coin, profile.coins.toString(), "Coins"),
        StatItem(ArrowMazeIcons.Hint, profile.hints.toString(), "Hints"),
        StatItem(ArrowMazeIcons.Life, profile.lives.toString(), "Lives"),
        StatItem(ArrowMazeIcons.Trophy, profile.gamesPlayed.toString(), "Played"),
        StatItem(ArrowMazeIcons.Trophy, profile.gamesWon.toString(), "Won"),
        StatItem(ArrowMazeIcons.Trophy, profile.winRate.toPercentString(), "Win Rate"),
        StatItem(ArrowMazeIcons.Trophy, profile.bestStreak.toString(), "Best Streak"),
        StatItem(ArrowMazeIcons.Trophy, profile.averageSolveTimeMs.formatDuration(), "Avg Solve"),
        StatItem(ArrowMazeIcons.Trophy, profile.highestLevel.toString(), "Top Level"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
        stats.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(
                            icon = item.icon,
                            value = item.value,
                            label = item.label,
                        )
                    }
                }
                // Pad to 3 columns when the row is short.
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Equipped cosmetics row: theme + arrow skin + trail FX chips. */
@Composable
private fun CosmeticsRow(profile: Profile) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        Text(
            text = "Equipped",
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        ) {
            CosmeticChip(label = "Theme", value = profile.currentThemeId)
            CosmeticChip(label = "Arrow", value = profile.currentArrowSkinId)
            CosmeticChip(label = "Trail", value = profile.currentTrailFxId)
        }
    }
}

/** Small cosmetic preview chip used by [CosmeticsRow]. Fills its row cell. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.CosmeticChip(
    label: String,
    value: String,
) {
    val cs = MaterialTheme.colorScheme
    Box(modifier = Modifier.weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(cs.surfaceVariant.copy(alpha = 0.7f))
                .padding(SpacingTokens.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Compact action button with leading icon + label. Fills its row cell via [RowScope.weight]. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.weight(1f)) {
        ArrowMazeButton(
            text = label,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Tonal,
            leadingIcon = icon,
        )
    }
}

/** Simple value holder for the stats grid. */
private data class StatItem(
    val icon: ImageVector,
    val value: String,
    val label: String,
)

/** Formats the join-date epoch ms as "MMM yyyy" in the user's locale. */
private fun formatJoinDate(epochMs: Long): String {
    val date = Date(epochMs)
    val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return fmt.format(date)
}
