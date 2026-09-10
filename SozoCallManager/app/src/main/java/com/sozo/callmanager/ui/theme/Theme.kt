package com.sozo.callmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SozoBlue = Color(0xFF1D5FBF)
private val SozoBlueDark = Color(0xFF0E3E86)
private val SozoTeal = Color(0xFF00A896)
private val SozoBackground = Color(0xFFF6F8FB)

private val LightColors = lightColorScheme(
    primary = SozoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FB),
    onPrimaryContainer = SozoBlueDark,
    secondary = SozoTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF5F2),
    onSecondaryContainer = Color(0xFF04342C),
    background = SozoBackground,
    onBackground = Color(0xFF20242C),
    surface = Color.White,
    onSurface = Color(0xFF20242C),
    surfaceVariant = Color(0xFFEDF0F5),
    onSurfaceVariant = Color(0xFF5F6672),
    outline = Color(0xFFD7DBE3),
    outlineVariant = Color(0xFFE8EAEF),
    error = Color(0xFFD64545)
)

private val DarkColors = darkColorScheme(
    primary = SozoBlue,
    onPrimary = Color.White,
    secondary = SozoTeal,
    onSecondary = Color.White,
    background = Color(0xFF10141A),
    surface = Color(0xFF1B2028),
    surfaceVariant = Color(0xFF262C36),
    outline = Color(0xFF3A414C),
    error = Color(0xFFFF6B6B)
)

/**
 * Rounder, softer corners app-wide — this single change is what makes cards,
 * buttons, chips, and text fields across every screen feel more "modern"
 * without having to touch each screen individually.
 */
private val SozoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SozoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, shapes = SozoShapes, content = content)
}

val BrandBlueDark = SozoBlueDark
