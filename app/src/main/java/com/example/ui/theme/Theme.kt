package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FridayColorScheme = darkColorScheme(
    primary = FridayCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = FridayNeonBlue,
    onSecondary = Color(0xFF002C6F),
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = FridayPurple,
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = FridayDarkBg,
    onBackground = FridayTextPrimary,
    surface = FridayDarkSurface,
    onSurface = FridayTextPrimary,
    surfaceVariant = FridayDarkSurfaceContainer,
    onSurfaceVariant = FridayTextSecondary,
    outline = FridayBorder,
    error = FridayRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FridayColorScheme,
        typography = Typography,
        content = content
    )
}
