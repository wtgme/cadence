package io.cadence.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary          = GlowDefault,
    onPrimary        = Color.Black,
    primaryContainer = Color(0xFF1A2E1A),
    secondary        = GlowResting,
    onSecondary      = Color.Black,
    background       = Surface0,
    surface          = Surface1,
    surfaceVariant   = Surface2,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline          = SurfaceBorder,
    error            = ErrorRed,
)

@Composable
fun CadenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = CadenceTypography,
        content     = content,
    )
}
