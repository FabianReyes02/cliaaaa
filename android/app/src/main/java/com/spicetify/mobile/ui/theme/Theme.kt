package com.spicetify.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spicetify.mobile.data.AppSettings
import com.spicetify.mobile.data.ThemePresets

/**
 * Builds the Material 3 color scheme from the selected preset, the custom
 * accent and (optionally) the color extracted from the current cover art.
 */
@Composable
fun SpicetifyTheme(
    settings: AppSettings,
    artworkAccent: Int?,
    content: @Composable () -> Unit,
) {
    val preset = ThemePresets.byId(settings.themeId)

    val accent = when {
        settings.matchArtwork && artworkAccent != null -> Color(artworkAccent)
        settings.customAccentArgb != null -> Color(settings.customAccentArgb)
        else -> preset.accent
    }

    val background = if (settings.amoled) Color.Black else preset.background
    val surface = if (settings.amoled) Color(0xFF0B0B0B) else preset.surface
    val secondary = if (settings.amoled) accent else preset.secondary

    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = onColor(accent),
        primaryContainer = accent.copy(alpha = 0.20f).compositeOver(surface),
        onPrimaryContainer = accent,
        inversePrimary = accent,
        secondary = secondary,
        onSecondary = onColor(secondary),
        secondaryContainer = secondary.copy(alpha = 0.18f).compositeOver(surface),
        onSecondaryContainer = secondary,
        tertiary = secondary,
        onTertiary = onColor(secondary),
        background = background,
        onBackground = Color(0xFFF4F4F4),
        surface = surface,
        onSurface = Color(0xFFF1F1F1),
        surfaceVariant = Color(0xFF242424),
        onSurfaceVariant = Color(0xFFB0B0B0),
        outline = Color(0xFF3A3A3A),
        outlineVariant = Color(0xFF2A2A2A),
        error = Color(0xFFFF6B6B),
        onError = Color.Black,
        errorContainer = Color(0xFF3A1414),
        onErrorContainer = Color(0xFFFFB4B4),
        surfaceTint = accent,
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = SpicetifyTypography,
        shapes = SpicetifyShapes,
        content = content,
    )
}

private fun onColor(color: Color): Color =
    if (color.luminance() > 0.5f) Color.Black else Color.White

private val SpicetifyTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

private val SpicetifyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
