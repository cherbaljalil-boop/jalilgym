package com.chdjalil.jalilgym.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

internal val DarkColors = darkColorScheme(
    primary = JgPalette.Lime,
    onPrimary = JgPalette.NearBlack,
    primaryContainer = JgPalette.LimeContainerDark,
    onPrimaryContainer = JgPalette.OnLimeContainerDark,
    secondary = JgPalette.SteelBlue,
    onSecondary = Color.White,
    secondaryContainer = JgPalette.SteelContainerDark,
    onSecondaryContainer = JgPalette.SteelContainerLight,
    background = JgPalette.NearBlack,
    onBackground = JgPalette.OnSurfaceDark,
    surface = JgPalette.SurfaceDark,
    onSurface = JgPalette.OnSurfaceDark,
    surfaceVariant = JgPalette.SurfaceContainerHighDark,
    onSurfaceVariant = JgPalette.OnSurfaceVariantDark,
    surfaceContainerLowest = JgPalette.NearBlack,
    surfaceContainerLow = JgPalette.SurfaceDark,
    surfaceContainer = JgPalette.SurfaceContainerDark,
    surfaceContainerHigh = JgPalette.SurfaceContainerHighDark,
    surfaceContainerHighest = JgPalette.OutlineDark,
    outline = JgPalette.OutlineDark,
    outlineVariant = JgPalette.SurfaceContainerHighDark,
    error = JgPalette.ErrorDark,
    onError = JgPalette.NearBlack,
)

internal val LightColors = lightColorScheme(
    primary = JgPalette.LimeDark,
    onPrimary = Color.White,
    primaryContainer = JgPalette.Lime,
    onPrimaryContainer = JgPalette.OnLimeContainerLight,
    secondary = JgPalette.SteelBlueDark,
    onSecondary = Color.White,
    secondaryContainer = JgPalette.SteelContainerLight,
    onSecondaryContainer = JgPalette.SteelContainerDark,
    background = JgPalette.BackgroundLight,
    onBackground = JgPalette.OnSurfaceLight,
    surface = JgPalette.SurfaceLight,
    onSurface = JgPalette.OnSurfaceLight,
    surfaceVariant = JgPalette.SurfaceContainerHighLight,
    onSurfaceVariant = JgPalette.OnSurfaceVariantLight,
    surfaceContainerLowest = JgPalette.SurfaceLight,
    surfaceContainerLow = JgPalette.BackgroundLight,
    surfaceContainer = JgPalette.SurfaceContainerLight,
    surfaceContainerHigh = JgPalette.SurfaceContainerHighLight,
    surfaceContainerHighest = JgPalette.OutlineLight,
    outline = JgPalette.OutlineLight,
    outlineVariant = JgPalette.SurfaceContainerHighLight,
    error = JgPalette.ErrorLight,
    onError = Color.White,
)

/**
 * Jalilgym theme. Dark by default; dynamic color is intentionally not used
 * so the brand palette stays consistent across devices.
 */
@Composable
fun JalilgymTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalJgExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JgTypography,
            shapes = JgShapes,
            content = content,
        )
    }
}

/** Access to brand colors outside the Material 3 roles: `JgTheme.extendedColors.success`. */
object JgTheme {
    val extendedColors: JgExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalJgExtendedColors.current
}
