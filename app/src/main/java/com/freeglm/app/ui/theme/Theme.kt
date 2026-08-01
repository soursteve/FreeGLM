package com.freeglm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = SendBgActiveDark,
    onPrimary = SendIconActiveDark,
    background = BackgroundDark,
    surface = BackgroundDark,
    surfaceVariant = PillDark,
    onBackground = OnDark,
    onSurface = OnDark,
    onSurfaceVariant = OnMutedDark
)

private val LightColors = lightColorScheme(
    primary = SendBgActiveLight,
    onPrimary = SendIconActiveLight,
    background = BackgroundLight,
    surface = BackgroundLight,
    surfaceVariant = PillLight,
    onBackground = OnLight,
    onSurface = OnLight,
    onSurfaceVariant = OnMutedLight
)

@Composable
fun FreeGLMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
