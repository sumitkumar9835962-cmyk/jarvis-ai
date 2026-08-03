package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = DarkObsidian,
    primaryContainer = TechSurfaceVariant,
    onPrimaryContainer = TextCyanLight,
    secondary = NeonBlue,
    onSecondary = Color.White,
    secondaryContainer = ElectricPurple.copy(alpha = 0.3f),
    onSecondaryContainer = TextCyanLight,
    tertiary = AmberGold,
    background = DarkObsidian,
    onBackground = TextCyanLight,
    surface = TechSurface,
    onSurface = TextCyanLight,
    surfaceVariant = TechSurfaceVariant,
    onSurfaceVariant = TextCyanLight.copy(alpha = 0.8f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}

