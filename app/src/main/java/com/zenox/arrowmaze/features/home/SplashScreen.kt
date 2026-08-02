package com.zenox.arrowmaze.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.designsystem.icons.ArrowMazeIcons
import com.zenox.arrowmaze.core.designsystem.theme.BrandBlue
import com.zenox.arrowmaze.core.designsystem.theme.BrandViolet
import com.zenox.arrowmaze.core.designsystem.tokens.SpacingTokens
import com.zenox.arrowmaze.core.firebase.auth.ArrowMazeAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-injected [ViewModel] backing the [SplashScreen].
 *
 * Exposes a single hot stream — [routing] — that resolves to:
 *  - `SplashRouting.Loading` while the auth state + the 1-second minimum
 *    splash delay are still in flight.
 *  - `SplashRouting.GoHome` when both the Firebase user is non-null AND
 *    the user has previously completed the auth flow (cold-start
 *    restoration of a signed-in user).
 *  - `SplashRouting.GoAuth` when the user is null OR the auth flow hasn't
 *    completed yet (fresh install / signed out).
 *
 * The decision is sticky: once it flips away from [SplashRouting.Loading]
 * it never re-evaluates, so the user sees a single deterministic
 * transition (no flicker if Firebase emits `null` momentarily during
 * init).
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    auth: ArrowMazeAuth,
    sessionRepository: SessionRepository,
) : ViewModel() {

    val routing: StateFlow<SplashRouting> = combine(
        auth.currentUser,
        sessionRepository.hasCompletedAuthFlow,
    ) { user, hasCompleted ->
        when {
            user != null && hasCompleted -> SplashRouting.GoHome
            user == null && hasCompleted -> SplashRouting.GoAuth
            // While auth is being restored from disk (hasCompleted == false),
            // wait for the user snapshot to settle. Once it does, route
            // accordingly. The 1s min-delay is enforced by the screen so
            // the splash brand frame is visible even on instant decisions.
            user != null -> SplashRouting.Loading
            else -> SplashRouting.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SplashRouting.Loading,
    )
}

/** Routing decision surfaced by [SplashViewModel]. */
sealed interface SplashRouting {
    data object Loading : SplashRouting
    data object GoHome : SplashRouting
    data object GoAuth : SplashRouting
}

/**
 * Full-screen splash surface. Renders the brand logo (Target icon in a
 * brand-gradient disc) + "Arrow Maze" wordmark + a small indeterminate
 * spinner. A [LaunchedEffect] observes the [SplashViewModel.routing]
 * stream and — once the routing decision is non-`Loading` AND the 1-second
 * minimum splash delay has elapsed — calls [onNavigateToHome] or
 * [onNavigateToAuth].
 *
 * @param onNavigateToHome Called when the user is already authenticated.
 * @param onNavigateToAuth Called when the user needs to sign in / sign up.
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val routing by viewModel.routing.collectAsStateWithLifecycle()
    var minDelayElapsed by remember { mutableStateOf(false) }

    // 1-second minimum splash visibility so the brand frame is readable
    // even on instant routing decisions.
    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_MS)
        minDelayElapsed = true
    }

    LaunchedEffect(routing, minDelayElapsed) {
        if (!minDelayElapsed) return@LaunchedEffect
        when (routing) {
            SplashRouting.GoHome -> {
                Timber.d("Splash → Home")
                onNavigateToHome()
            }
            SplashRouting.GoAuth -> {
                Timber.d("Splash → Auth")
                onNavigateToAuth()
            }
            SplashRouting.Loading -> {
                // Still waiting for auth state to settle.
            }
        }
    }

    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(colors = listOf(BrandBlue, BrandViolet))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(SpacingTokens.xl),
        ) {
            // Brand logo disc
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ArrowMazeIcons.Target,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp),
                )
            }
            Spacer(Modifier.height(SpacingTokens.xl))
            Text(
                text = "Arrow Maze",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(Modifier.height(SpacingTokens.lg))
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private const val MIN_SPLASH_MS = 1_000L
