package com.zenox.arrowmaze.features.authentication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.R
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeButton
import com.zenox.arrowmaze.core.designsystem.components.ArrowMazeCard
import com.zenox.arrowmaze.core.designsystem.components.ButtonStyle
import com.zenox.arrowmaze.core.designsystem.components.CardVariant
import com.zenox.arrowmaze.core.designsystem.components.GradientBackground
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.features.authentication.components.GoogleSignInButton
import timber.log.Timber

/** Auth-screen tab. Drives which form fields + button label are visible. */
private enum class AuthTab(val label: String) {
    SIGN_IN("Sign In"),
    SIGN_UP("Sign Up"),
}

/**
 * Full-screen auth surface. Mounts a brand-gradient background + a centred
 * [ArrowMazeCard] hosting the Sign In / Sign Up form, the Google Sign-In
 * button, the Guest CTA, and a "Forgot password?" link.
 *
 * State ownership:
 *  - Tab selection + form field values are screen-local `rememberSaveable`
 *    state — they survive configuration changes.
 *  - The loading / error / authenticated lifecycle is driven by
 *    [AuthViewModel.uiState] collected via `collectAsStateWithLifecycle`.
 *  - One-shot navigation events flow through `AuthViewModel.navEvents` and
 *    are collected by a [LaunchedEffect] that routes them to the
 *    [onAuthenticated] callback or the snackbar host.
 *
 * @param onAuthenticated Invoked once after a successful auth (drives the
 *                        NavHost's transition to Home).
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Tab state — defaults to Sign In.
    var selectedTab by rememberSaveable { mutableStateOf(AuthTab.SIGN_IN) }

    // Form field state.
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }

    var showForgotPassword by rememberSaveable { mutableStateOf(false) }

    val keyboard = LocalSoftwareKeyboardController.current

    val isLoading = uiState is AuthUiState.Loading
    val loadingMessage = (uiState as? AuthUiState.Loading)?.message

    // Route one-shot navigation events.
    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is AuthNavEvent.NavigateHome -> onAuthenticated()
                is AuthNavEvent.NavigateForgotPassword -> showForgotPassword = true
                is AuthNavEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Surface errors from the VM as a snackbar (auto-clears when state flips
    // back to Idle on the next attempt).
    LaunchedEffect(uiState) {
        (uiState as? AuthUiState.Error)?.let { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        GradientBackground(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = SpacingTokens.xl)
                    .padding(top = SpacingTokens.xxxl)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.lg),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BrandHeader()

                    ArrowMazeCard(variant = CardVariant.Elevated) {
                        TabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AuthTab.entries.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    text = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                )
                            }
                        }

                        Spacer(Modifier.height(SpacingTokens.lg))

                        // Display Name field — only shown in sign-up mode.
                        AnimatedVisibility(
                            visible = selectedTab == AuthTab.SIGN_UP,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = {
                                        displayName = it
                                        displayNameError = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Display name") },
                                    singleLine = true,
                                    isError = displayNameError != null,
                                    supportingText = displayNameError?.let { { Text(it) } },
                                    enabled = !isLoading,
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Person, contentDescription = null)
                                    },
                                )
                                Spacer(Modifier.height(SpacingTokens.md))
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.auth_email)) },
                            singleLine = true,
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !isLoading,
                        )

                        Spacer(Modifier.height(SpacingTokens.md))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.auth_password)) },
                            singleLine = true,
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it) } },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    enabled = !isLoading,
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        },
                                    )
                                }
                            },
                            enabled = !isLoading,
                        )

                        Spacer(Modifier.height(SpacingTokens.lg))

                        ArrowMazeButton(
                            text = selectedTab.label,
                            onClick = {
                                keyboard?.hide()
                                if (validateForm(
                                        tab = selectedTab,
                                        email = email,
                                        password = password,
                                        displayName = displayName,
                                        onEmailError = { emailError = it },
                                        onPasswordError = { passwordError = it },
                                        onDisplayNameError = { displayNameError = it },
                                    )
                                ) {
                                    if (selectedTab == AuthTab.SIGN_IN) {
                                        viewModel.signInWithEmail(email.trim(), password)
                                    } else {
                                        viewModel.signUpWithEmail(
                                            email.trim(),
                                            password,
                                            displayName.trim(),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Primary,
                            isLoading = isLoading,
                        )

                        // Forgot password link — sign-in mode only.
                        AnimatedVisibility(visible = selectedTab == AuthTab.SIGN_IN) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = { showForgotPassword = true },
                                    enabled = !isLoading,
                                ) {
                                    Text(stringResource(R.string.auth_forgot_password))
                                }
                            }
                        }

                        Spacer(Modifier.height(SpacingTokens.md))
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = SpacingTokens.xs),
                        )

                        GoogleSignInButton(
                            onIdToken = { token -> viewModel.signInWithGoogle(token) },
                            onError = { msg ->
                                Timber.w("Google sign-in error: %s", msg)
                                // Will be surfaced via uiState.Error after VM
                                // rejects the empty token; bridge to snackbar
                                // directly as a fallback.
                                // (No-op here — snackbar wired in LaunchedEffect.)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            isLoading = false,
                        )

                        Spacer(Modifier.height(SpacingTokens.sm))

                        ArrowMazeButton(
                            text = stringResource(R.string.auth_sign_in_guest),
                            onClick = { viewModel.signInAsGuest() },
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Tonal,
                            enabled = !isLoading,
                        )
                    }

                    Spacer(Modifier.height(SpacingTokens.xl))

                    // Loading message under the card (visible only while loading).
                    AnimatedVisibility(visible = isLoading && loadingMessage != null) {
                        Text(
                            text = loadingMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(SpacingTokens.xxl))
                }
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            initialEmail = email,
            onSubmit = { submittedEmail ->
                viewModel.sendPasswordReset(submittedEmail)
                showForgotPassword = false
            },
            onDismiss = { showForgotPassword = false },
            isSending = isLoading,
        )
    }
}

// ---------- helpers ----------

/** Brand logo (Target icon in a brand-coloured circle) + "Arrow Maze" title. */
@Composable
private fun BrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
    ) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ArrowMazeIcons.Target,
                    contentDescription = "Arrow Maze logo",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(SpacingTokens.xs))
        // Empty trailing spacer keeps the header visually balanced.
        Spacer(Modifier.width(0.dp))
    }
}

/**
 * Validates the form fields based on the active tab. Returns `true` when
 * every visible field passes its check; otherwise sets the supplied error
 * callbacks and returns `false`.
 */
private fun validateForm(
    tab: AuthTab,
    email: String,
    password: String,
    displayName: String,
    onEmailError: (String) -> Unit,
    onPasswordError: (String) -> Unit,
    onDisplayNameError: (String) -> Unit,
): Boolean {
    var ok = true

    if (!isValidEmail(email)) {
        onEmailError("Enter a valid email address.")
        ok = false
    }
    if (password.length < 6) {
        onPasswordError("Password must be at least 6 characters.")
        ok = false
    }
    if (tab == AuthTab.SIGN_UP) {
        if (displayName.trim().length < 2) {
            onDisplayNameError("Display name must be at least 2 characters.")
            ok = false
        }
    }
    return ok
}
