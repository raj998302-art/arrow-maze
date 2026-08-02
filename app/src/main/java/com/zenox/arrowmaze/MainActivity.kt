package com.zenox.arrowmaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.arrowmaze.core.designsystem.theme.ArrowMazeTheme
import com.zenox.arrowmaze.core.navigation.ArrowMazeNavHost
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Single-activity host for the entire Jetpack Compose UI.
 *
 * CRASH-SAFE MODE: The entire Compose tree is wrapped in a runCatching
 * guard. If any composable throws during composition (e.g. a missing
 * resource, a bad vector path, a null state access), the app does NOT
 * crash — it shows a minimal "Safe Mode" screen with a Retry button
 * instead. This ensures the user never sees a silent close.
 *
 * Startup logging: every major init step is logged to Timber so the
 * exact failing step is visible in logcat.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("STEP: MainActivity.onCreate — installing splash screen.")
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        Timber.i("STEP: MainActivity.onCreate — enabling edge-to-edge.")
        enableEdgeToEdge()

        // Keep splash on screen until the first frame is ready, then fade out.
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        Timber.i("STEP: MainActivity.onCreate — mounting Compose tree.")
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
                CrashSafeHost()
            }
        }
        Timber.i("STEP: MainActivity.onCreate — Compose tree mounted, dismissing splash.")
        keepSplash = false
    }
}

/**
 * Wraps the NavHost in a try-catch boundary. If the NavHost or any
 * destination composable throws during composition, the exception is
 * caught and a minimal fallback screen is shown with a Retry button.
 *
 * This is the "TEMPORARY SAFETY MODE" requested in the crash fix spec:
 * the app must never close silently — it always shows SOMETHING.
 */
@Composable
private fun CrashSafeHost() {
    var crashState by remember { mutableStateOf<Throwable?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    val crash = crashState
    if (crash != null) {
        Timber.e(crash, "CRASH-SAFE: Compose tree crashed — showing fallback screen.")
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = crash.message ?: "Unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            crashState = null
                            retryKey++
                            Timber.i("CRASH-SAFE: User tapped Retry — recomposing.")
                        },
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        return
    }

    // Keyed by retryKey so a Retry tap forces full recomposition.
    // The runCatching is a second defense layer — if the NavHost itself
    // throws during composition (not caught by Compose's normal error
    // handling), we catch it here.
    androidx.compose.runtime.key(retryKey) {
        runCatching {
            ArrowMazeNavHost()
        }.onFailure { t ->
            Timber.e(t, "CRASH-SAFE: NavHost threw — switching to fallback screen.")
            crashState = t
        }
    }
}
