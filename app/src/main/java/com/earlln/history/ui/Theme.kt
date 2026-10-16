package com.earlln.history.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A3E1D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DFCC),
    onPrimaryContainer = Color(0xFF3A1B08),
    secondary = Color(0xFF8A6A2B),
    secondaryContainer = Color(0xFFF5E6C4),
    onSecondaryContainer = Color(0xFF2E2208),
    tertiary = Color(0xFF2F5D62),
    tertiaryContainer = Color(0xFFD3E8EA),
    onTertiaryContainer = Color(0xFF0E2A2D),
    background = Color(0xFFFBF7F1),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFBF7F1),
    onSurface = Color(0xFF221A14),
    surfaceVariant = Color(0xFFF1E7DB),
    onSurfaceVariant = Color(0xFF5A4A3E),
    surfaceContainer = Color(0xFFF4ECE2),
    surfaceContainerHigh = Color(0xFFEFE5D8),
    outlineVariant = Color(0xFFDCCBB8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0B58C),
    onPrimary = Color(0xFF4A2109),
    primaryContainer = Color(0xFF5E2F14),
    onPrimaryContainer = Color(0xFFFFDCC6),
    secondary = Color(0xFFDCC48C),
    secondaryContainer = Color(0xFF4A3B17),
    onSecondaryContainer = Color(0xFFF5E6C4),
    tertiary = Color(0xFF9CCDD2),
    tertiaryContainer = Color(0xFF17464B),
    onTertiaryContainer = Color(0xFFD3E8EA),
    background = Color(0xFF1A1512),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF1A1512),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF3A3029),
    onSurfaceVariant = Color(0xFFD5C3B3),
    surfaceContainer = Color(0xFF241D18),
    surfaceContainerHigh = Color(0xFF2E2620),
    outlineVariant = Color(0xFF52443A),
)

@Composable
fun EarllnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
