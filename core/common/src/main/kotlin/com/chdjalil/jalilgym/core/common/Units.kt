package com.chdjalil.jalilgym.core.common

/**
 * Unit conversions. Values are always stored in metric (kg, cm);
 * conversion happens only when formatting for display or parsing input.
 */
object Units {
    const val LB_PER_KG = 2.2046226218
    const val CM_PER_INCH = 2.54

    fun kgToLb(kg: Double): Double = kg * LB_PER_KG

    fun lbToKg(lb: Double): Double = lb / LB_PER_KG

    fun cmToInches(cm: Double): Double = cm / CM_PER_INCH

    fun inchesToCm(inches: Double): Double = inches * CM_PER_INCH
}
