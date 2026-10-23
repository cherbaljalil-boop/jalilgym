package com.chdjalil.jalilgym.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.chdjalil.jalilgym.core.designsystem.R

/** Condensed athletic face for screen titles, timers and weight/rep numerals. */
val BebasNeue = FontFamily(Font(R.font.bebas_neue_regular, FontWeight.Normal))

@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Inter (variable) for all running text. */
val Inter = FontFamily(
    interWeight(FontWeight.Normal),
    interWeight(FontWeight.Medium),
    interWeight(FontWeight.SemiBold),
    interWeight(FontWeight.Bold),
)

/** Tabular figures so numbers don't jitter in columns and timers. */
private const val TABULAR = "tnum"

private fun display(size: Int, line: Int) = TextStyle(
    fontFamily = BebasNeue,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.01.em,
    fontFeatureSettings = TABULAR,
)

private fun inter(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = TABULAR,
)

internal val JgTypography = Typography(
    displayLarge = display(64, 64),
    displayMedium = display(52, 52),
    displaySmall = display(40, 44),
    headlineLarge = display(36, 40),
    headlineMedium = display(30, 34),
    headlineSmall = display(26, 30),
    titleLarge = inter(20, 28, FontWeight.SemiBold),
    titleMedium = inter(16, 24, FontWeight.SemiBold, 0.01),
    titleSmall = inter(14, 20, FontWeight.SemiBold, 0.01),
    bodyLarge = inter(16, 24, FontWeight.Normal),
    bodyMedium = inter(14, 20, FontWeight.Normal),
    bodySmall = inter(12, 16, FontWeight.Normal, 0.01),
    labelLarge = inter(14, 20, FontWeight.SemiBold, 0.01),
    labelMedium = inter(12, 16, FontWeight.Medium, 0.03),
    labelSmall = inter(11, 16, FontWeight.Medium, 0.04),
)
