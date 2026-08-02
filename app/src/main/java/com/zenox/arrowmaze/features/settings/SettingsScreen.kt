package com.zenox.arrowmaze.features.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.BuildConfig
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeCard
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeIconButton
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.ErrorState
import com.zenox.arrowmaze.core.designsystem.components.LoadingState
import com.zenox.arrowmaze.core.designsystem.components.SectionHeader
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeDarkMode
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.settings.components.DropdownOption
import com.zenox.arrowmaze.features.settings.components.DropdownRow
import com.zenox.arrowmaze.features.settings.components.SliderRow
import com.zenox.arrowmaze.features.settings.components.ThemePickerRow
import com.zenox.arrowmaze.features.settings.components.ToggleRow

/**
 * Root composable for the Settings screen.
 *
 * Sections (each in an [ArrowMazeCard]):
 *  - Appearance: dark mode segmented control, theme picker, high-contrast
 *    toggle, color-blind dropdown, font-size slider.
 *  - Audio: music volume, SFX volume, vibration toggle.
 *  - Notifications: enable-toggle.
 *  - Account (if signed in): email-verification status + resend, sign-out,
 *    delete-account (with confirmation dialog).
 *  - About: app version + privacy/terms links.
 *
 * Every setting persists immediately via [SettingsViewModel]; appearance
 * changes also propagate to the live theme through [ThemeManager].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
                SettingsUiEvent.SignedOut, SettingsUiEvent.AccountDeleted -> onSignedOut()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    ArrowMazeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_button),
                        onClick = onBack,
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
                is SettingsUiState.Loading -> LoadingState(message = "Loading settings…")
                is SettingsUiState.Error -> ErrorState(message = state.message)
                is SettingsUiState.Success -> SettingsContent(
                    state = state,
                    viewModel = viewModel,
                    onSignedOut = onSignedOut,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Success,
    viewModel: SettingsViewModel,
    onSignedOut: () -> Unit,
) {
    val settings = state.settings
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SpacingTokens.md),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.md),
    ) {
        // ---------- Appearance ----------
        ArrowMazeCard(
            header = { SectionHeader(title = "Appearance", leadingIcon = Icons.Rounded.Palette) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
                // Dark mode segmented control
                Text(
                    text = "Dark mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = ArrowMazeDarkMode.entries
                    modes.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = settings.darkMode == mode.name,
                            onClick = { viewModel.setDarkMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                        ) {
                            Text(when (mode) {
                                ArrowMazeDarkMode.SYSTEM -> "System"
                                ArrowMazeDarkMode.LIGHT -> "Light"
                                ArrowMazeDarkMode.DARK -> "Dark"
                            })
                        }
                    }
                }

                Spacer(Modifier.height(SpacingTokens.xs))

                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                ThemePickerRow(
                    selectedThemeId = settings.themeId,
                    onThemeSelected = viewModel::setTheme,
                )

                ToggleRow(
                    label = "High contrast",
                    subtitle = "Increases board / arrow stroke contrast for visibility.",
                    checked = settings.highContrast,
                    onCheckedChange = viewModel::setHighContrast,
                    leadingIcon = Icons.Rounded.Visibility,
                )

                DropdownRow(
                    label = "Color blind mode",
                    subtitle = "Adjusts palette for color vision deficiencies.",
                    selectedValue = settings.colorBlindMode,
                    options = listOf(
                        DropdownOption("NONE", "None"),
                        DropdownOption("PROTANOPIA", "Protanopia (red-blind)"),
                        DropdownOption("DEUTERANOPIA", "Deuteranopia (green-blind)"),
                        DropdownOption("TRITANOPIA", "Tritanopia (blue-blind)"),
                    ),
                    onValueChange = viewModel::setColorBlindMode,
                )

                SliderRow(
                    label = "Font size",
                    value = settings.fontScale,
                    valueText = "${(settings.fontScale * 100).toInt()}%",
                    onValueChange = viewModel::setFontScale,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    subtitle = "Scales all in-app text.",
                )
            }
        }

        // ---------- Audio ----------
        ArrowMazeCard(
            header = { SectionHeader(title = "Audio", leadingIcon = Icons.Rounded.VolumeUp) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
                SliderRow(
                    label = "Music volume",
                    value = settings.musicVolume.toFloat(),
                    valueText = "${settings.musicVolume}%",
                    onValueChange = { viewModel.setMusicVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                )
                SliderRow(
                    label = "Sound effects volume",
                    value = settings.sfxVolume.toFloat(),
                    valueText = "${settings.sfxVolume}%",
                    onValueChange = { viewModel.setSfxVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                )
                ToggleRow(
                    label = "Vibration",
                    subtitle = "Haptic feedback on cell rotation and wins.",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = viewModel::setVibration,
                    leadingIcon = Icons.Rounded.Vibration,
                )
            }
        }

        // ---------- Notifications ----------
        ArrowMazeCard(
            header = { SectionHeader(title = "Notifications", leadingIcon = Icons.Rounded.Notifications) },
        ) {
            ToggleRow(
                label = "Enable notifications",
                subtitle = "Daily challenge reminders, friend requests, and reward alerts.",
                checked = settings.notificationsEnabled,
                onCheckedChange = viewModel::setNotifications,
            )
        }

        // ---------- Account ----------
        val authUser = state.authUser
        if (authUser != null) {
            ArrowMazeCard(
                header = { SectionHeader(title = "Account", leadingIcon = Icons.Rounded.AccountCircle) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
                    // Email verification status
                    if (!authUser.isAnonymous) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = if (authUser.isEmailVerified) {
                                        Icons.Rounded.MarkEmailRead
                                    } else {
                                        Icons.Rounded.Email
                                    },
                                    contentDescription = null,
                                    tint = if (authUser.isEmailVerified) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                )
                                Column {
                                    Text(
                                        text = authUser.email ?: "No email on file",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = if (authUser.isEmailVerified) {
                                            "Email verified"
                                        } else {
                                            "Email not verified"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (!authUser.isEmailVerified) {
                                ArrowMazeButton(
                                    text = "Resend",
                                    onClick = viewModel::sendEmailVerification,
                                    style = ButtonStyle.Tonal,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(SpacingTokens.xs))

                    ArrowMazeButton(
                        text = stringResource(R.string.auth_logout),
                        onClick = viewModel::signOut,
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Tonal,
                        leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                    )
                    ArrowMazeButton(
                        text = stringResource(R.string.auth_delete_account),
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Outline,
                        leadingIcon = Icons.Rounded.Delete,
                    )
                }
            }
        }

        // ---------- About ----------
        ArrowMazeCard(
            header = { SectionHeader(title = "About", leadingIcon = Icons.Rounded.Info) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { openUrl(context, "https://arrowmaze.zenox.com/privacy") },
                ) {
                    Text("Privacy policy")
                }
                TextButton(
                    onClick = { openUrl(context, "https://arrowmaze.zenox.com/terms") },
                ) {
                    Text("Terms of service")
                }
            }
        }

        Spacer(Modifier.height(SpacingTokens.xxl))
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

/** Opens [url] in the system browser. */
private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
