package com.zenox.arrowmaze.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zenox.arrowmaze.core.domain.model.GameTheme

/**
 * Safely unwraps a [Context] (which may be a [ContextWrapper] such as
 * `ContextThemeWrapper` or `MutableContextWrapper` used by Compose under
 * edge-to-edge / `Scaffold`) to find the hosting [Activity].
 *
 * Returns `null` if no Activity is in the wrapper chain — callers MUST
 * handle that (the system bar colour update is cosmetic and should be
 * skipped, not crashed on, when the Activity isn't reachable).
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    scrim = LightScrim,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    scrim = DarkScrim,
)

/**
 * Game-specific palette (arrow / trail / goal colors). Overridden per-theme
 * in Phase 8 by the ThemeManager. Defaults to the light/dark game palette.
 */
val LocalGamePalette = staticCompositionLocalOf { LightGamePalette }

/**
 * Currently active visual theme id (e.g. "light", "dark", "neon"). Used by
 * feature screens that need to switch custom backgrounds.
 */
val LocalThemeId = compositionLocalOf { "light" }

/**
 * Whether the user has high-contrast accessibility enabled.
 */
val LocalHighContrast = staticCompositionLocalOf { false }

enum class ArrowMazeDarkMode { SYSTEM, LIGHT, DARK }

/**
 * Root theme composable.
 *
 * @param darkMode user preference (system / light / dark). Defaults to system.
 * @param dynamicColor whether to use Material You wallpaper colors on Android 12+.
 *                     Disabled by default to preserve the brand blue/violet identity.
 * @param themeId the active cosmetic theme id (Phase 8). For Phase 2, only "light"/"dark".
 * @param highContrast accessibility flag.
 * @param gameTheme optional [GameTheme] used to derive the Material 3 [ColorScheme]
 *                  + [GamePalette]. When non-null, overrides the default
 *                  light/dark palettes with the cosmetic theme's tokens
 *                  (Phase 8 ThemeManager integration).
 * @param content screen content.
 */
@Composable
fun ArrowMazeTheme(
    darkMode: ArrowMazeDarkMode = ArrowMazeDarkMode.SYSTEM,
    dynamicColor: Boolean = false,
    themeId: String = "light",
    highContrast: Boolean = false,
    gameTheme: GameTheme? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedDark = when (darkMode) {
        ArrowMazeDarkMode.SYSTEM -> systemDark
        ArrowMazeDarkMode.LIGHT -> false
        ArrowMazeDarkMode.DARK -> true
    }

    val colorScheme = when {
        gameTheme != null -> gameTheme.toColorScheme()
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (resolvedDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        resolvedDark -> DarkColors
        else -> LightColors
    }

    val gamePalette = when {
        gameTheme != null && !highContrast -> gameTheme.toGamePalette()
        highContrast -> if (resolvedDark) DarkGamePalette else LightGamePalette
        resolvedDark -> DarkGamePalette
        else -> LightGamePalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Safely unwrap the Activity from the view context — it may be
            // wrapped in a ContextThemeWrapper (especially under edge-to-edge
            // + Scaffold). A direct `as Activity` cast crashes with
            // ClassCastException on certain OEM roms / wrapper setups.
            val activity = view.context.findActivity()
            if (activity != null) {
                try {
                    @Suppress("DEPRECATION")
                    activity.window.statusBarColor = colorScheme.background.toArgb()
                    @Suppress("DEPRECATION")
                    activity.window.navigationBarColor = colorScheme.background.toArgb()
                    WindowCompat.getInsetsController(activity.window, view).apply {
                        isAppearanceLightStatusBars = !resolvedDark
                        isAppearanceLightNavigationBars = !resolvedDark
                    }
                } catch (t: Throwable) {
                    // Status/nav bar color is cosmetic — never crash the app for it.
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalGamePalette provides gamePalette,
        LocalThemeId provides themeId,
        LocalHighContrast provides highContrast,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ArrowMazeTypography,
            shapes = ArrowMazeShapes,
            content = content,
        )
    }
}
