package com.example.contadorhoras.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary            = CambridgeBlue,
    onPrimary          = Charcoal,
    primaryContainer   = Celadon,
    onPrimaryContainer = Charcoal,

    secondary          = Asparagus,
    onSecondary        = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = CambridgeBlue,
    onSecondaryContainer = Charcoal,

    tertiary           = Charcoal,
    onTertiary         = androidx.compose.ui.graphics.Color.White,

    background         = LavenderBlush,
    onBackground       = Charcoal,
    surface            = LavenderBlush,
    onSurface          = Charcoal,
    surfaceVariant     = Celadon,
    onSurfaceVariant   = Charcoal,
    outline            = OutlineDark
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary            = Celadon,
    onPrimary          = Charcoal,
    primaryContainer   = CambridgeBlue,
    onPrimaryContainer = Charcoal,

    secondary          = Asparagus,
    onSecondary        = Charcoal,
    secondaryContainer = Asparagus,
    onSecondaryContainer = LavenderBlush,

    tertiary           = LavenderBlush,
    onTertiary         = Charcoal,

    background         = Charcoal,
    onBackground       = LavenderBlush,
    surface            = Charcoal,
    onSurface          = LavenderBlush,
    surfaceVariant     = CambridgeBlue,
    onSurfaceVariant   = LavenderBlush,
    outline            = Celadon
)

@Composable
fun ContadorHorasTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography, // si ya definiste tu Typography (Anton, etc.)
        content = content
    )
}
