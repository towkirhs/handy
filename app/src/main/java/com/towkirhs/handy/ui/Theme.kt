package com.towkirhs.handy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B5BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E5FF),
    onPrimaryContainer = Color(0xFF241C66),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF17171C),
    surface = Color.White,
    onSurface = Color(0xFF17171C),
    surfaceVariant = Color(0xFFF1EDE6),
    onSurfaceVariant = Color(0xFF5E5A66),
    outlineVariant = Color(0xFFE6E1D8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA79BFF),
    onPrimary = Color(0xFF1B1440),
    primaryContainer = Color(0xFF3A3170),
    onPrimaryContainer = Color(0xFFE9E5FF),
    background = Color(0xFF121214),
    onBackground = Color(0xFFF2EFEA),
    surface = Color(0xFF1C1C21),
    onSurface = Color(0xFFF2EFEA),
    surfaceVariant = Color(0xFF26262C),
    onSurfaceVariant = Color(0xFFB5B0BE),
    outlineVariant = Color(0xFF34343C)
)

@Composable
fun HandyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
