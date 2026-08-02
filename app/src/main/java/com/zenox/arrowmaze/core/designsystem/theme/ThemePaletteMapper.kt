package com.zenox.arrowmaze.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.zenox.arrowmaze.core.domain.model.GameTheme

/**
 * Converts a domain [GameTheme] into a Material 3 [ColorScheme].
 *
 * Material 3 roles are derived from the 15-token [com.zenox.arrowmaze.core.domain.model.ThemeColors]
 * palette:
 *  - `primary` / `secondary` / `tertiary` map straight across.
 *  - Containers are computed by lightening (light) or darkening (dark) the
 *    source hue. We use Compose's `Color.copy(alpha = …)` + `lerp` for this.
 *  - `background` / `surface` / `onBackground` / `onSurface` map straight across.
 *  - `error` defaults to a brand red unless the theme is "neon" / "cyberpunk"
 *    (where it borrows the primary pink so the palette stays cohesive).
 *
 * The mapping is deterministic per [GameTheme.id]; the same theme always
 * produces the same [ColorScheme].
 */
fun GameTheme.toColorScheme(): ColorScheme {
    val primary = colors.primary.toColorOrNull() ?: BrandBlue
    val secondary = colors.secondary.toColorOrNull() ?: BrandViolet
    val tertiary = colors.tertiary.toColorOrNull() ?: LightTertiary
    val background = colors.background.toColorOrNull()
        ?: if (isDark) DarkBackground else LightBackground
    val surface = colors.surface.toColorOrNull()
        ?: if (isDark) DarkSurface else LightSurface
    val onBackground = colors.onBackground.toColorOrNull()
        ?: if (isDark) DarkOnBackground else LightOnBackground
    val onSurface = colors.onSurface.toColorOrNull()
        ?: if (isDark) DarkOnSurface else LightOnSurface

    val primaryContainer = primary.blend(if (isDark) Color.Black else Color.White, 0.65f)
    val onPrimaryContainer = primary.blend(if (isDark) Color.White else Color.Black, 0.85f)
    val secondaryContainer = secondary.blend(if (isDark) Color.Black else Color.White, 0.65f)
    val onSecondaryContainer = secondary.blend(if (isDark) Color.White else Color.Black, 0.85f)
    val tertiaryContainer = tertiary.blend(if (isDark) Color.Black else Color.White, 0.65f)
    val onTertiaryContainer = tertiary.blend(if (isDark) Color.White else Color.Black, 0.85f)

    val surfaceVariant = surface.blend(if (isDark) Color.White else Color.Black, 0.10f)
    val onSurfaceVariant = onSurface.blend(if (isDark) Color.White else Color.Black, 0.25f)
    val outline = onSurfaceVariant
    val outlineVariant = surface.blend(if (isDark) Color.White else Color.Black, 0.20f)

    val error = when (id) {
        "neon", "cyberpunk" -> primary
        "golden" -> Color(0xFFFF5252)
        else -> if (isDark) DarkError else LightError
    }
    val onError = if (isDark) DarkOnError else LightOnError
    val errorContainer = error.blend(if (isDark) Color.Black else Color.White, 0.70f)
    val onErrorContainer = error.blend(if (isDark) Color.White else Color.Black, 0.85f)
    val scrim = Color(0xFF000000)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = primary.blend(Color.White, 0.85f),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = secondary.blend(Color.White, 0.85f),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = tertiary.blend(Color.White, 0.85f),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            scrim = scrim,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = primary.blend(Color.White, 0.85f),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = secondary.blend(Color.White, 0.85f),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = tertiary.blend(Color.White, 0.85f),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            scrim = scrim,
        )
    }
}

/**
 * Converts a domain [GameTheme] into a [GamePalette] used by the in-game
 * Canvas renderer (arrows, trail, goal halo, etc.).
 *
 * Falls back to the built-in light/dark palettes when the theme's hex token
 * is missing or unparsable so the game never crashes on a malformed theme.
 */
fun GameTheme.toGamePalette(): GamePalette {
    val arrowFill = colors.arrowFill.toColorOrNull() ?: BrandBlue
    val arrowOutline = arrowFill.blend(Color.Black, 0.45f)
    val trailStart = colors.trailStart.toColorOrNull() ?: arrowFill
    val trailEnd = colors.trailEnd.toColorOrNull() ?: BrandViolet
    val goalFill = colors.goalFill.toColorOrNull() ?: Color(0xFFFFC107)
    val goalGlow = goalFill.blend(Color.White, 0.30f)
    val startFill = colors.startFill.toColorOrNull() ?: Color(0xFF00C853)
    val startGlow = startFill.blend(Color.White, 0.30f)
    val cellEmpty = colors.cellEmpty.toColorOrNull()
        ?: if (isDark) Color(0xFF141A30) else Color(0xFFF4F7FF)
    val cellTapped = colors.cellTapped.toColorOrNull()
        ?: if (isDark) Color(0xFF243156) else Color(0xFFE3ECFF)
    val boardFrame = colors.boardFrame.toColorOrNull()
        ?: if (isDark) Color(0xFF2C3658) else Color(0xFFC9D3F0)
    return GamePalette(
        arrowFill = arrowFill,
        arrowOutline = arrowOutline,
        trailStart = trailStart,
        trailEnd = trailEnd,
        goalFill = goalFill,
        goalGlow = goalGlow,
        startFill = startFill,
        startGlow = startGlow,
        cellEmpty = cellEmpty,
        cellTapped = cellTapped,
        boardFrame = boardFrame,
    )
}

/** Parses a `"#RRGGBB"` / `"#AARRGGBB"` hex string into a [Color], or null. */
private fun String.toColorOrNull(): Color? = runCatching {
    val hex = removePrefix("#")
    when (hex.length) {
        6 -> Color(color = hex.toLong(16) or 0xFF000000L)
        8 -> Color(color = hex.toLong(16))
        else -> null
    }
}.getOrNull()

/**
 * Blends this colour with [other] by [fraction] (0 = unchanged, 1 = fully
 * [other]). Wraps `androidx.compose.ui.graphics.lerp` for ergonomics.
 */
private fun Color.blend(other: Color, fraction: Float): Color =
    androidx.compose.ui.graphics.lerp(this, other, fraction.coerceIn(0f, 1f))
