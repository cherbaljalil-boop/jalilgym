package com.chdjalil.jalilgym.core.domain.workout

import kotlin.math.roundToInt

enum class OneRepMaxFormula { EPLEY, BRZYCKI }

/** Above this many reps the estimates get noticeably less reliable. */
const val ONE_REP_MAX_RELIABLE_REPS = 12

/**
 * Estimated one-rep max (docs/BLUEPRINT.md §3.4). Returns null when there's
 * nothing to estimate from. Brzycki is undefined at 37+ reps.
 */
fun estimateOneRepMax(weight: Double, reps: Int, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): Double? {
    if (reps <= 0 || weight <= 0.0) return null
    if (reps == 1) return weight
    return when (formula) {
        OneRepMaxFormula.EPLEY -> weight * (1 + reps / 30.0)
        OneRepMaxFormula.BRZYCKI -> if (reps >= 37) null else weight * 36.0 / (37 - reps)
    }
}

/** Training-max table: percentage → weight, for 50–95% in 5% steps. */
fun percentTable(oneRepMax: Double): List<Pair<Int, Double>> =
    (50..95 step 5).map { percent -> percent to oneRepMax * percent / 100.0 }

/** Rounds to the nearest multiple of [step] (e.g. 2.5 kg plates in pairs). */
fun roundToStep(value: Double, step: Double): Double =
    if (step <= 0.0) value else (value / step).roundToInt() * step
