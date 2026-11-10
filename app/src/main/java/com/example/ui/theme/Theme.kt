package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PcHubDarkColorScheme = darkColorScheme(
    primary = PcHubCyan,
    onPrimary = Color.Black,
    secondary = PcHubBlue,
    onSecondary = Color.White,
    background = PcHubBackground,
    onBackground = PcHubTextPrimary,
    surface = PcHubSurface,
    onSurface = PcHubTextPrimary,
    surfaceVariant = PcHubSurfaceVariant,
    onSurfaceVariant = PcHubTextSecondary,
    outline = PcHubBorder,
    error = PcHubError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PcHubDarkColorScheme,
        typography = Typography,
        content = content
    )
}
