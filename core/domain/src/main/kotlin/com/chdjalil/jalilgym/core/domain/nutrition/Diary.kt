package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Nutrients
import java.time.LocalDate
import kotlin.math.abs

/** Longest run of consecutive days with at least one log entry. */
fun bestLoggingStreak(loggedDates: Collection<LocalDate>): Int {
    val days = loggedDates.toSortedSet()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in days) {
        run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}

/** Current run ending today, or yesterday if today has nothing logged yet. */
fun currentLoggingStreak(loggedDates: Collection<LocalDate>, today: LocalDate): Int {
    val days = loggedDates.toSet()
    var day = if (today in days) today else today.minusDays(1)
    var run = 0
    while (day in days) {
        run++
        day = day.minusDays(1)
    }
    return run
}

/** Calories within this fraction of target count as on target. */
const val CALORIE_TOLERANCE = 0.10

/** Protein at or above this fraction of target counts as hit. */
const val PROTEIN_HIT_FRACTION = 0.90

/** A day hits its targets when calories are within ±10% and protein reaches 90%. */
fun hitTargets(totals: Nutrients, targets: MacroTargets): Boolean {
    if (targets.calories <= 0) return false
    val caloriesOk = abs(totals.kcal - targets.calories) <= targets.calories * CALORIE_TOLERANCE
    val proteinOk = totals.proteinG >= targets.proteinG * PROTEIN_HIT_FRACTION
    return caloriesOk && proteinOk
}

/** Clamps a 0..∞ progress ratio for rings and bars. */
fun progressFraction(value: Double, target: Number): Float {
    val t = target.toDouble()
    return if (t <= 0) 0f else (value / t).coerceIn(0.0, 1.0).toFloat()
}
