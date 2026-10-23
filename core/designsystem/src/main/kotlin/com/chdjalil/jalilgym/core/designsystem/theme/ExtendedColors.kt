package com.chdjalil.jalilgym.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Brand colors that have no Material 3 role. */
@Immutable
data class JgExtendedColors(
    val success: Color,
    val warning: Color,
    val setWarmup: Color,
    val setWorking: Color,
    val setDrop: Color,
    val setFailure: Color,
)

internal val DarkExtendedColors = JgExtendedColors(
    success = JgPalette.SuccessDark,
    warning = JgPalette.WarningDark,
    setWarmup = JgPalette.SteelBlue,
    setWorking = JgPalette.Lime,
    setDrop = JgPalette.WarningDark,
    setFailure = JgPalette.ErrorDark,
)

internal val LightExtendedColors = JgExtendedColors(
    success = JgPalette.SuccessLight,
    warning = JgPalette.WarningLight,
    setWarmup = JgPalette.SteelBlueDark,
    setWorking = JgPalette.LimeDark,
    setDrop = JgPalette.WarningLight,
    setFailure = JgPalette.ErrorLight,
)

val LocalJgExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
