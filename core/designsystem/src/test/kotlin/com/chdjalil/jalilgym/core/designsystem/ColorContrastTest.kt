package com.chdjalil.jalilgym.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.chdjalil.jalilgym.core.designsystem.theme.DarkColors
import com.chdjalil.jalilgym.core.designsystem.theme.LightColors
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** Guards the blueprint's WCAG AA requirement (§2.2) for the core color pairs. */
class ColorContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val l1 = maxOf(a.luminance(), b.luminance()) + 0.05
        val l2 = minOf(a.luminance(), b.luminance()) + 0.05
        return (l1 / l2).toDouble()
    }

    @Test
    fun corePairsMeetAa() {
        for ((name, scheme) in listOf("dark" to DarkColors, "light" to LightColors)) {
            val pairs = mapOf(
                "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                "onBackground/background" to (scheme.onBackground to scheme.background),
                "onSurface/surface" to (scheme.onSurface to scheme.surface),
                "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
                "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "onSecondaryContainer/secondaryContainer" to
                    (scheme.onSecondaryContainer to scheme.secondaryContainer),
            )
            pairs.forEach { (pair, colors) ->
                assertWithMessage("$name $pair")
                    .that(contrast(colors.first, colors.second))
                    .isAtLeast(4.5)
            }
        }
    }
}
