package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Sex
import java.time.LocalDate
import java.time.Period
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val KCAL_PER_G_PROTEIN = 4.0
const val KCAL_PER_G_CARBS = 4.0
const val KCAL_PER_G_FAT = 9.0

/** Energy in one kg of body weight change, used for the weekly-loss cap. */
const val KCAL_PER_KG_BODY_WEIGHT = 7700.0

/** At most 1% of body weight lost per week (docs/BLUEPRINT.md §3.7). */
const val MAX_WEEKLY_LOSS_FRACTION = 0.01

fun minimumCalories(sex: Sex): Int = when (sex) {
    Sex.MALE -> 1500
    Sex.FEMALE -> 1200
}

fun ageOn(birthDate: LocalDate, today: LocalDate): Int = Period.between(birthDate, today).years

/** Mifflin-St Jeor basal metabolic rate, kcal/day. */
fun mifflinStJeorBmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
    val base = 10 * weightKg + 6.25 * heightCm - 5 * age
    return base + if (sex == Sex.MALE) 5 else -161
}

fun computeTdee(profile: BodyProfile, weightKg: Double, today: LocalDate): Double =
    mifflinStJeorBmr(profile.sex, weightKg, profile.heightCm, ageOn(profile.birthDate, today)) * profile.activityLevel.factor

/** Why a target was adjusted for safety; the UI explains each one. */
enum class TargetClamp { CALORIE_FLOOR, WEEKLY_LOSS_CAP }

data class TargetResult(val targets: MacroTargets, val tdee: Int, val clamps: Set<TargetClamp>)

/**
 * Daily targets from TDEE (docs/BLUEPRINT.md §3.7): goal sets calories,
 * protein is grams per kg (2.4 g/kg for the high-protein style), fat is a share
 * of calories but at least 0.6 g/kg, and carbs fill the rest.
 */
fun computeTargets(goal: DietGoal, style: DietStyle, tdee: Double, weightKg: Double, sex: Sex): TargetResult {
    val clamps = mutableSetOf<TargetClamp>()
    var calories = when (goal) {
        DietGoal.CUT -> {
            val planned = min(tdee * 0.20, 750.0)
            val cap = weightKg * MAX_WEEKLY_LOSS_FRACTION * KCAL_PER_KG_BODY_WEIGHT / 7
            if (cap < planned) clamps += TargetClamp.WEEKLY_LOSS_CAP
            tdee - min(planned, cap)
        }
        DietGoal.LEAN_BULK -> tdee + min(tdee * 0.10, 400.0)
        DietGoal.MAINTAIN -> tdee
    }
    val floor = minimumCalories(sex).toDouble()
    if (calories < floor) {
        calories = floor
        clamps += TargetClamp.CALORIE_FLOOR
    }
    val roundedCalories = (calories / 10).roundToInt() * 10

    val proteinPerKg = when {
        style == DietStyle.HIGH_PROTEIN -> 2.4
        goal == DietGoal.CUT -> 2.2
        goal == DietGoal.LEAN_BULK -> 1.8
        else -> 2.0
    }
    val fatShare = if (goal == DietGoal.MAINTAIN && style == DietStyle.BALANCED) 0.27 else 0.25
    val protein = (proteinPerKg * weightKg).roundToInt()
    val fat = max(roundedCalories * fatShare / KCAL_PER_G_FAT, 0.6 * weightKg).roundToInt()
    val carbs = max(0.0, (roundedCalories - protein * KCAL_PER_G_PROTEIN - fat * KCAL_PER_G_FAT) / KCAL_PER_G_CARBS).roundToInt()
    return TargetResult(MacroTargets(roundedCalories, protein, carbs, fat), tdee.roundToInt(), clamps)
}

/** Calories implied by macros, for showing manual overrides consistently. */
fun caloriesFromMacros(proteinG: Int, carbsG: Int, fatG: Int): Int =
    (proteinG * KCAL_PER_G_PROTEIN + carbsG * KCAL_PER_G_CARBS + fatG * KCAL_PER_G_FAT).roundToInt()
