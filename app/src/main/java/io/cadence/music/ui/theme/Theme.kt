package io.cadence.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary           = CadenceBlue,
    onPrimary         = CadenceBg,
    primaryContainer  = CadenceBlueDimHi,
    onPrimaryContainer = CadenceText,
    secondary         = CadenceOrange,
    onSecondary       = CadenceBg,
    secondaryContainer = CadenceOrangeDimHi,
    onSecondaryContainer = CadenceText,
    tertiary          = CadenceOrange,
    onTertiary        = CadenceBg,
    background        = CadenceBg,
    onBackground      = CadenceText,
    surface           = CadenceSurface,
    onSurface         = CadenceText,
    surfaceVariant    = CadenceSurfaceHi,
    onSurfaceVariant  = CadenceTextMute,
    outline           = CadenceBorderHi,
    outlineVariant    = CadenceBorder,
    error             = CadenceRed,
    onError           = CadenceBg,
)

@Composable
fun CadenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = CadenceTypography,
        content     = content,
    )
}
