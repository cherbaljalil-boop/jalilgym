package com.chdjalil.jalilgym.core.domain.hydration

import com.chdjalil.jalilgym.core.model.UnitSystem
import kotlin.math.roundToInt

/** Used when there's no body weight to go on. */
const val DEFAULT_WATER_GOAL_ML = 2_500

/** Goals the user can set by hand. */
val WATER_GOAL_RANGE_ML = 500..6_000

const val ML_PER_FL_OZ = 29.5735

private const val ML_PER_KG = 35
private const val GOAL_STEP_ML = 250
private val AUTOMATIC_GOAL_RANGE_ML = 1_500..4_000

/**
 * About 35 ml per kg of body weight, rounded to a 250 ml glass and kept
 * between 1.5 and 4 litres. This is a rough everyday guide, not advice:
 * training, heat and food all change how much someone needs.
 */
fun automaticWaterGoalMl(weightKg: Double?): Int {
    if (weightKg == null || weightKg <= 0.0) return DEFAULT_WATER_GOAL_ML
    val rounded = (weightKg * ML_PER_KG / GOAL_STEP_ML).roundToInt() * GOAL_STEP_ML
    return rounded.coerceIn(AUTOMATIC_GOAL_RANGE_ML)
}

/** The user's own goal if they set one, otherwise the automatic one. */
fun waterGoalMl(customGoalMl: Int?, weightKg: Double?): Int = customGoalMl ?: automaticWaterGoalMl(weightKg)

/** One-tap amounts: a glass and a bottle, in sizes that are familiar for each unit system. */
fun quickWaterAmountsMl(units: UnitSystem): List<Int> = when (units) {
    UnitSystem.METRIC -> listOf(250, 500)
    UnitSystem.IMPERIAL -> listOf(flOzToMl(8.0), flOzToMl(16.0))
}

fun flOzToMl(flOz: Double): Int = (flOz * ML_PER_FL_OZ).roundToInt()

fun mlToFlOz(ml: Int): Double = ml / ML_PER_FL_OZ
