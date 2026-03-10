package com.callmind.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CallMindDarkScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = TextPrimary,
    primaryContainer = GreenDark,
    onPrimaryContainer = TextPrimary,
    secondary = GreenPrimary,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkDivider,
    error = CallMissed,
    onError = TextPrimary
)

@Composable
fun CallMindTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CallMindDarkScheme,
        typography = Typography,
        content = content
    )
}
