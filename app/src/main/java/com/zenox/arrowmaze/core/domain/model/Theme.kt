package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Palette tokens for a single theme. Every field is a hex string (e.g. `"#1A1A2E"`) so the
 * structure is portable across platforms and serialisable without any Android colour deps.
 *
 * The names are intentionally domain-specific (arrowFill, trailStart, …) rather than
 * generic Material3 names, because the game paints many bespoke surfaces (arrows, trail
 * gradient, goal halo, etc.) that don't map cleanly to Material roles.
 */
@Serializable
data class ThemeColors(
    @SerialName("primary")       val primary: String,
    @SerialName("secondary")     val secondary: String,
    @SerialName("tertiary")      val tertiary: String,
    @SerialName("background")    val background: String,
    @SerialName("surface")       val surface: String,
    @SerialName("onBackground")  val onBackground: String,
    @SerialName("onSurface")     val onSurface: String,
    @SerialName("arrowFill")     val arrowFill: String,
    @SerialName("trailStart")    val trailStart: String,
    @SerialName("trailEnd")      val trailEnd: String,
    @SerialName("goalFill")      val goalFill: String,
    @SerialName("startFill")     val startFill: String,
    @SerialName("cellEmpty")     val cellEmpty: String,
    @SerialName("cellTapped")    val cellTapped: String,
    @SerialName("boardFrame")    val boardFrame: String
)

/**
 * A full cosmetic theme. The UI looks up [GameTheme] by [id] from [ALL_THEMES] and maps the
 * [ThemeColors] hex strings into Compose `Color` values via the design-system layer.
 *
 * @property id           Stable unique id (e.g. `"cyberpunk"`).
 * @property displayName  Player-facing name shown in the theme picker.
 * @property isDark       Whether the palette is dark (controls status bar icons etc.).
 * @property isPremium    True if only Premium subscribers can select this theme.
 * @property price        Coin price if the theme is unlockable via coins; 0 if free/default.
 * @property colors       The actual palette.
 */
@Serializable
data class GameTheme(
    @SerialName("id")          val id: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("isDark")      val isDark: Boolean,
    @SerialName("isPremium")   val isPremium: Boolean,
    @SerialName("price")       val price: Int,
    @SerialName("colors")      val colors: ThemeColors
) {
    companion object {
        /** Canonical 13-theme catalogue shipped with the app. */
        val ALL_THEMES: List<GameTheme> = listOf(
            GameTheme(
                id = "light", displayName = "Light", isDark = false, isPremium = false, price = 0,
                colors = ThemeColors(
                    primary = "#3F51B5", secondary = "#5C6BC0", tertiary = "#FFB300",
                    background = "#FAFAFA", surface = "#FFFFFF",
                    onBackground = "#1A1A1A", onSurface = "#212121",
                    arrowFill = "#3F51B5", trailStart = "#5C6BC0", trailEnd = "#7E57C2",
                    goalFill = "#4CAF50", startFill = "#FFB300",
                    cellEmpty = "#E0E0E0", cellTapped = "#FFD54F", boardFrame = "#9E9E9E"
                )
            ),
            GameTheme(
                id = "dark", displayName = "Dark", isDark = true, isPremium = false, price = 0,
                colors = ThemeColors(
                    primary = "#7986CB", secondary = "#9575CD", tertiary = "#FFB74D",
                    background = "#121212", surface = "#1E1E1E",
                    onBackground = "#EEEEEE", onSurface = "#E0E0E0",
                    arrowFill = "#7986CB", trailStart = "#9575CD", trailEnd = "#64B5F6",
                    goalFill = "#66BB6A", startFill = "#FFB74D",
                    cellEmpty = "#2C2C2C", cellTapped = "#FFD54F", boardFrame = "#424242"
                )
            ),
            GameTheme(
                id = "cyberpunk", displayName = "Cyberpunk", isDark = true, isPremium = true, price = 250,
                colors = ThemeColors(
                    primary = "#FF00E5", secondary = "#00F0FF", tertiary = "#FFE600",
                    background = "#0A0014", surface = "#1A0033",
                    onBackground = "#F0F0FF", onSurface = "#E0E0FF",
                    arrowFill = "#00F0FF", trailStart = "#FF00E5", trailEnd = "#FFE600",
                    goalFill = "#39FF14", startFill = "#FF6EC7",
                    cellEmpty = "#1A0033", cellTapped = "#FF00E5", boardFrame = "#9D00FF"
                )
            ),
            GameTheme(
                id = "minimal", displayName = "Minimal", isDark = false, isPremium = false, price = 0,
                colors = ThemeColors(
                    primary = "#222222", secondary = "#555555", tertiary = "#888888",
                    background = "#FFFFFF", surface = "#F5F5F5",
                    onBackground = "#111111", onSurface = "#222222",
                    arrowFill = "#222222", trailStart = "#555555", trailEnd = "#888888",
                    goalFill = "#222222", startFill = "#BBBBBB",
                    cellEmpty = "#EEEEEE", cellTapped = "#666666", boardFrame = "#CCCCCC"
                )
            ),
            GameTheme(
                id = "glass", displayName = "Glass", isDark = true, isPremium = true, price = 300,
                colors = ThemeColors(
                    primary = "#73C2FB", secondary = "#A3C1E8", tertiary = "#B5E8FF",
                    background = "#0F1B2D", surface = "#1E2F4A",
                    onBackground = "#EAF6FF", onSurface = "#D9EEFF",
                    arrowFill = "#73C2FB", trailStart = "#A3C1E8", trailEnd = "#B5E8FF",
                    goalFill = "#4FD1C5", startFill = "#90CDF4",
                    cellEmpty = "#2A3F5F", cellTapped = "#B5E8FF", boardFrame = "#5A7BA3"
                )
            ),
            GameTheme(
                id = "neon", displayName = "Neon", isDark = true, isPremium = true, price = 350,
                colors = ThemeColors(
                    primary = "#FF073A", secondary = "#39FF14", tertiary = "#1F51FF",
                    background = "#0D0D0D", surface = "#1A1A1A",
                    onBackground = "#FFFFFF", onSurface = "#F0F0F0",
                    arrowFill = "#39FF14", trailStart = "#FF073A", trailEnd = "#1F51FF",
                    goalFill = "#FFD700", startFill = "#FF6EC7",
                    cellEmpty = "#262626", cellTapped = "#39FF14", boardFrame = "#FF073A"
                )
            ),
            GameTheme(
                id = "ocean", displayName = "Ocean", isDark = false, isPremium = false, price = 0,
                colors = ThemeColors(
                    primary = "#0288D1", secondary = "#26C6DA", tertiary = "#80DEEA",
                    background = "#E1F5FE", surface = "#FFFFFF",
                    onBackground = "#01314F", onSurface = "#014261",
                    arrowFill = "#0288D1", trailStart = "#26C6DA", trailEnd = "#80DEEA",
                    goalFill = "#00897B", startFill = "#FFB300",
                    cellEmpty = "#B3E5FC", cellTapped = "#4FC3F7", boardFrame = "#0277BD"
                )
            ),
            GameTheme(
                id = "sunset", displayName = "Sunset", isDark = false, isPremium = true, price = 200,
                colors = ThemeColors(
                    primary = "#FF7043", secondary = "#FF9800", tertiary = "#FFCA28",
                    background = "#FFF3E0", surface = "#FFE0B2",
                    onBackground = "#3E2723", onSurface = "#5D4037",
                    arrowFill = "#FF7043", trailStart = "#FF9800", trailEnd = "#FFCA28",
                    goalFill = "#E91E63", startFill = "#AB47BC",
                    cellEmpty = "#FFCCBC", cellTapped = "#FFAB91", boardFrame = "#BF360C"
                )
            ),
            GameTheme(
                id = "forest", displayName = "Forest", isDark = false, isPremium = false, price = 0,
                colors = ThemeColors(
                    primary = "#2E7D32", secondary = "#66BB6A", tertiary = "#A5D6A7",
                    background = "#F1F8E9", surface = "#DCEDC8",
                    onBackground = "#1B5E20", onSurface = "#2E7D32",
                    arrowFill = "#2E7D32", trailStart = "#66BB6A", trailEnd = "#A5D6A7",
                    goalFill = "#8D6E63", startFill = "#FFCA28",
                    cellEmpty = "#C5E1A5", cellTapped = "#9CCC65", boardFrame = "#558B2F"
                )
            ),
            GameTheme(
                id = "space", displayName = "Space", isDark = true, isPremium = true, price = 300,
                colors = ThemeColors(
                    primary = "#7E57C2", secondary = "#5C6BC0", tertiary = "#26C6DA",
                    background = "#050511", surface = "#0F0F23",
                    onBackground = "#E0E0FF", onSurface = "#C5CAE9",
                    arrowFill = "#7E57C2", trailStart = "#5C6BC0", trailEnd = "#26C6DA",
                    goalFill = "#FFD700", startFill = "#FF6EC7",
                    cellEmpty = "#1A1A35", cellTapped = "#B39DDB", boardFrame = "#3F2B6B"
                )
            ),
            GameTheme(
                id = "galaxy", displayName = "Galaxy", isDark = true, isPremium = true, price = 400,
                colors = ThemeColors(
                    primary = "#9C27B0", secondary = "#3F51B5", tertiary = "#E040FB",
                    background = "#0B0420", surface = "#160A36",
                    onBackground = "#F3E5F5", onSurface = "#E1BEE7",
                    arrowFill = "#E040FB", trailStart = "#9C27B0", trailEnd = "#3F51B5",
                    goalFill = "#FFD700", startFill = "#FF80AB",
                    cellEmpty = "#1F0E47", cellTapped = "#CE93D8", boardFrame = "#6A1B9A"
                )
            ),
            GameTheme(
                id = "golden", displayName = "Golden", isDark = true, isPremium = true, price = 500,
                colors = ThemeColors(
                    primary = "#FFD700", secondary = "#FFC107", tertiary = "#FF9800",
                    background = "#1A1400", surface = "#2E2400",
                    onBackground = "#FFF8E1", onSurface = "#FFECB3",
                    arrowFill = "#FFD700", trailStart = "#FFC107", trailEnd = "#FF9800",
                    goalFill = "#FFE082", startFill = "#FFAB00",
                    cellEmpty = "#3D2E00", cellTapped = "#FFCA28", boardFrame = "#8D6E00"
                )
            ),
            GameTheme(
                id = "wood", displayName = "Wood", isDark = false, isPremium = false, price = 0,
                colors = ThemeColors(
                    primary = "#8D6E63", secondary = "#A1887F", tertiary = "#D7CCC8",
                    background = "#EFEBE9", surface = "#D7CCC8",
                    onBackground = "#3E2723", onSurface = "#4E342E",
                    arrowFill = "#5D4037", trailStart = "#8D6E63", trailEnd = "#A1887F",
                    goalFill = "#6D4C41", startFill = "#FFA000",
                    cellEmpty = "#BCAAA4", cellTapped = "#A1887F", boardFrame = "#3E2723"
                )
            )
        )

        /** Lookup by id; falls back to the default dark theme if not found. */
        fun byId(id: String): GameTheme =
            ALL_THEMES.firstOrNull { it.id == id } ?: ALL_THEMES[1] // dark
    }
}
