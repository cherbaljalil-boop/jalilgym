package com.chdjalil.jalilgym.core.domain.units

import com.chdjalil.jalilgym.core.common.Units
import com.chdjalil.jalilgym.core.model.UnitSystem
import kotlin.math.abs
import kotlin.math.roundToLong

/** Weights are stored in kg; these convert at the UI boundary only. */
fun UnitSystem.fromKg(kg: Double): Double = when (this) {
    UnitSystem.METRIC -> kg
    UnitSystem.IMPERIAL -> Units.kgToLb(kg)
}

fun UnitSystem.toKg(value: Double): Double = when (this) {
    UnitSystem.METRIC -> value
    UnitSystem.IMPERIAL -> Units.lbToKg(value)
}

val UnitSystem.weightSymbol: String
    get() = when (this) {
        UnitSystem.METRIC -> "kg"
        UnitSystem.IMPERIAL -> "lb"
    }

/** Standard bar and plates for each unit system (in that unit). */
val UnitSystem.defaultBar: Double
    get() = when (this) {
        UnitSystem.METRIC -> 20.0
        UnitSystem.IMPERIAL -> 45.0
    }

val UnitSystem.barOptions: List<Double>
    get() = when (this) {
        UnitSystem.METRIC -> listOf(20.0, 15.0, 10.0)
        UnitSystem.IMPERIAL -> listOf(45.0, 35.0, 25.0)
    }

val UnitSystem.standardPlates: List<Double>
    get() = when (this) {
        UnitSystem.METRIC -> listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        UnitSystem.IMPERIAL -> listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    }

/** "82.5", "80", "181.9" — at most one decimal, no trailing ".0". */
fun formatNumber(value: Double): String {
    val tenths = (value * 10).roundToLong()
    return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${abs(tenths % 10)}"
}

/** Weight in the user's unit without the symbol, e.g. for input fields. */
fun UnitSystem.formatWeightValue(kg: Double): String = formatNumber(fromKg(kg))

/** Parses user input ("82,5" or "82.5") in this unit to kg; null when not a number. */
fun UnitSystem.parseWeightToKg(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }?.let(::toKg)
