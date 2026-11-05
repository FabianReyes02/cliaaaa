package com.spicetify.mobile.data

import androidx.compose.ui.graphics.Color

/**
 * Presets inspired by popular Spicetify marketplace themes.
 * The app is not a skin of Spotify, but it reuses the same visual language
 * so the "port" keeps the personality of the original project.
 */
data class ThemePreset(
    val id: String,
    val name: String,
    val accent: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
)

object ThemePresets {

    val SpotifyGreen = ThemePreset(
        id = "spotify-green",
        name = "Spotify Green",
        accent = Color(0xFF1DB954),
        secondary = Color(0xFF1ED760),
        background = Color(0xFF121212),
        surface = Color(0xFF181818),
    )

    val Dracula = ThemePreset(
        id = "dracula",
        name = "Dracula",
        accent = Color(0xFFBD93F9),
        secondary = Color(0xFFFF79C6),
        background = Color(0xFF282A36),
        surface = Color(0xFF21222C),
    )

    val Catppuccin = ThemePreset(
        id = "catppuccin",
        name = "Catppuccin",
        accent = Color(0xFFCBA6F7),
        secondary = Color(0xFF89B4FA),
        background = Color(0xFF1E1E2E),
        surface = Color(0xFF181825),
    )

    val Nord = ThemePreset(
        id = "nord",
        name = "Nord",
        accent = Color(0xFF88C0D0),
        secondary = Color(0xFF8FBCBB),
        background = Color(0xFF2E3440),
        surface = Color(0xFF3B4252),
    )

    val TokyoNight = ThemePreset(
        id = "tokyo-night",
        name = "Tokyo Night",
        accent = Color(0xFF7AA2F7),
        secondary = Color(0xFFBB9AF7),
        background = Color(0xFF1A1B26),
        surface = Color(0xFF16161E),
    )

    val Gruvbox = ThemePreset(
        id = "gruvbox",
        name = "Gruvbox",
        accent = Color(0xFFFABD2F),
        secondary = Color(0xFFFE8019),
        background = Color(0xFF282828),
        surface = Color(0xFF3C3836),
    )

    val all = listOf(SpotifyGreen, Dracula, Catppuccin, Nord, TokyoNight, Gruvbox)

    fun byId(id: String): ThemePreset = all.firstOrNull { it.id == id } ?: SpotifyGreen

    /** Accent swatches shown in Settings. */
    val accents: List<Color> = listOf(
        Color(0xFF1DB954),
        Color(0xFF8B5CF6),
        Color(0xFFFF4D8D),
        Color(0xFF3B82F6),
        Color(0xFF06B6D4),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFFA3E635),
    )
}
