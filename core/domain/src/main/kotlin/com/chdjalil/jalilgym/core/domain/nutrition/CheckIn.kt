package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.MacroTargets
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/** Size of one check-in adjustment (docs/BLUEPRINT.md §3.7). */
const val CHECK_IN_STEP_KCAL = 100

/** Days between check-ins. */
const val CHECK_IN_INTERVAL_DAYS = 7L

/** Weigh-ins needed in each of the two weeks compared. */
const val CHECK_IN_MIN_WEIGH_INS = 2

/** Healthy weekly change as % of body weight, by goal (negative = loss). */
fun goalRatePercent(goal: DietGoal): ClosedFloatingPointRange<Double> = when (goal) {
    DietGoal.CUT -> -1.0..-0.25
    DietGoal.LEAN_BULK -> 0.1..0.5
    DietGoal.MAINTAIN -> -0.25..0.25
}

enum class CheckInVerdict { NOT_ENOUGH_DATA, ON_TRACK, TOO_SLOW, TOO_FAST }

data class CheckIn(
    val verdict: CheckInVerdict,
    /** Average of the last 7 days minus the 7 days before, in kg; null without enough data. */
    val weeklyChangeKg: Double?,
    /** The same change as % of body weight. */
    val weeklyChangePercent: Double?,
    /** Suggested change to daily calories: −100, 0 or +100. */
    val adjustmentKcal: Int,
)

/**
 * Compares this week's average weight with last week's and suggests ±100 kcal
 * when the change is outside the goal's healthy range. "Too slow" means
 * further from the goal than intended (losing too little on a cut, gaining
 * too little on a bulk, drifting down on maintenance).
 */
fun weeklyCheckIn(weights: List<Pair<LocalDate, Double>>, goal: DietGoal, today: LocalDate): CheckIn {
    fun window(fromDaysAgo: Long, toDaysAgo: Long) = weights.filter { (date, _) ->
        !date.isBefore(today.minusDays(fromDaysAgo)) && !date.isAfter(today.minusDays(toDaysAgo))
    }.map { it.second }
    val recent = window(6, 0)
    val previous = window(13, 7)
    if (recent.size < CHECK_IN_MIN_WEIGH_INS || previous.size < CHECK_IN_MIN_WEIGH_INS) {
        return CheckIn(CheckInVerdict.NOT_ENOUGH_DATA, null, null, 0)
    }
    val change = recent.average() - previous.average()
    val percent = change / previous.average() * 100
    val range = goalRatePercent(goal)
    // Below the range means losing faster (or gaining slower) than intended: eat more.
    val (verdict, adjustment) = when {
        percent < range.start -> (if (goal == DietGoal.CUT) CheckInVerdict.TOO_FAST else CheckInVerdict.TOO_SLOW) to CHECK_IN_STEP_KCAL
        percent > range.endInclusive -> (if (goal == DietGoal.CUT) CheckInVerdict.TOO_SLOW else CheckInVerdict.TOO_FAST) to -CHECK_IN_STEP_KCAL
        else -> CheckInVerdict.ON_TRACK to 0
    }
    return CheckIn(verdict, change, percent, adjustment)
}

/** Whether a check-in is due: never done, or at least a week ago. */
fun checkInDue(lastCheckIn: LocalDate?, today: LocalDate): Boolean =
    lastCheckIn == null || !lastCheckIn.plusDays(CHECK_IN_INTERVAL_DAYS).isAfter(today)

/**
 * Moves calories by [deltaKcal] (never below [minimumKcal]); protein and fat
 * stay, carbs absorb the difference.
 */
fun adjustTargets(targets: MacroTargets, deltaKcal: Int, minimumKcal: Int): MacroTargets {
    val calories = max(targets.calories + deltaKcal, minimumKcal)
    val carbs = max(0, targets.carbsG + ((calories - targets.calories) / KCAL_PER_G_CARBS).roundToInt())
    return targets.copy(calories = calories, carbsG = carbs)
}
