package com.chdjalil.jalilgym.core.domain.workout

data class WarmupSet(val weight: Double, val reps: Int)

/**
 * Warm-up ramp for a working weight (docs/BLUEPRINT.md §3.4): empty bar × 10,
 * then 40% × 8, 60% × 5, 75% × 3, 85% × 1, each snapped to a loadable weight.
 * Steps at or below the bar weight, at or above the working weight, or that
 * repeat the previous weight are dropped.
 */
fun generateWarmups(workingWeight: Double, bar: Double, plates: List<Double>): List<WarmupSet> {
    if (workingWeight <= bar) return emptyList()
    val ramp = listOf(0.40 to 8, 0.60 to 5, 0.75 to 3, 0.85 to 1)
    val result = mutableListOf(WarmupSet(bar, 10))
    for ((fraction, reps) in ramp) {
        val weight = nearestLoadable(workingWeight * fraction, bar, plates)
        if (weight <= bar || weight >= workingWeight || weight <= result.last().weight) continue
        result += WarmupSet(weight, reps)
    }
    return result
}
