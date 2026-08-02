package com.zenox.arrowmaze.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ---- Brand ----
val BrandBlue = Color(0xFF3B6CFF)
val BrandViolet = Color(0xFF7B4DFF)
val BrandBlueDark = Color(0xFF2A52E5)
val BrandVioletDark = Color(0xFF6A3DE0)

// ---- Light scheme ----
val LightPrimary = BrandBlue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDCE4FF)
val LightOnPrimaryContainer = Color(0xFF001A41)
val LightSecondary = BrandViolet
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8DEFF)
val LightOnSecondaryContainer = Color(0xFF25005A)
val LightTertiary = Color(0xFF00BFA5)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFF9CF2E0)
val LightOnTertiaryContainer = Color(0xFF00201A)
val LightBackground = Color(0xFFEEF3FF)
val LightOnBackground = Color(0xFF16213D)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF16213D)
val LightSurfaceVariant = Color(0xFFE2E8F5)
val LightOnSurfaceVariant = Color(0xFF44474F)
val LightOutline = Color(0xFF74777F)
val LightOutlineVariant = Color(0xFFC4C6D0)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightScrim = Color(0xFF000000)

// ---- Dark scheme ----
val DarkPrimary = Color(0xFFB0C6FF)
val DarkOnPrimary = Color(0xFF002E6B)
val DarkPrimaryContainer = Color(0xFF1A44A8)
val DarkOnPrimaryContainer = Color(0xFFDCE4FF)
val DarkSecondary = Color(0xFFCFBCFF)
val DarkOnSecondary = Color(0xFF3A0092)
val DarkSecondaryContainer = Color(0xFF5524BA)
val DarkOnSecondaryContainer = Color(0xFFE8DEFF)
val DarkTertiary = Color(0xFF80D5C2)
val DarkOnTertiary = Color(0xFF00382E)
val DarkTertiaryContainer = Color(0xFF005143)
val DarkOnTertiaryContainer = Color(0xFF9CF2E0)
val DarkBackground = Color(0xFF0D1424)
val DarkOnBackground = Color(0xFFEAF0FF)
val DarkSurface = Color(0xFF1A2340)
val DarkOnSurface = Color(0xFFEAF0FF)
val DarkSurfaceVariant = Color(0xFF44474F)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
val DarkOutline = Color(0xFF8E9099)
val DarkOutlineVariant = Color(0xFF44474F)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkScrim = Color(0xFF000000)

// ---- Game-specific colors (passed via CompositionLocal, theme-dependent) ----
data class GamePalette(
    val arrowFill: Color,
    val arrowOutline: Color,
    val trailStart: Color,
    val trailEnd: Color,
    val goalFill: Color,
    val goalGlow: Color,
    val startFill: Color,
    val startGlow: Color,
    val cellEmpty: Color,
    val cellTapped: Color,
    val boardFrame: Color,
)

val LightGamePalette = GamePalette(
    arrowFill = BrandBlue,
    arrowOutline = Color(0xFF1A3B8F),
    trailStart = BrandBlue,
    trailEnd = BrandViolet,
    goalFill = Color(0xFFFFC107),
    goalGlow = Color(0xFFFFD54F),
    startFill = Color(0xFF00C853),
    startGlow = Color(0xFF69F0AE),
    cellEmpty = Color(0xFFF4F7FF),
    cellTapped = Color(0xFFE3ECFF),
    boardFrame = Color(0xFFC9D3F0),
)

val DarkGamePalette = GamePalette(
    arrowFill = Color(0xFF7FA0FF),
    arrowOutline = Color(0xFFB0C6FF),
    trailStart = Color(0xFF7FA0FF),
    trailEnd = Color(0xFFCFBCFF),
    goalFill = Color(0xFFFFD54F),
    goalGlow = Color(0xFFFFE082),
    startFill = Color(0xFF69F0AE),
    startGlow = Color(0xFFB9F6CA),
    cellEmpty = Color(0xFF141A30),
    cellTapped = Color(0xFF243156),
    boardFrame = Color(0xFF2C3658),
)
