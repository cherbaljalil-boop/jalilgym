package com.chdjalil.jalilgym.core.domain.workout

import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgressionOutcome
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutSet

/** Persisted per program exercise between sessions. */
data class ProgressionState(
    val nextWeightKg: Double,
    val nextTargetReps: Int,
    val consecutiveFailures: Int,
)

data class ProgressionResult(
    val outcome: ProgressionOutcome,
    /** The weight the working sets were done with this session. */
    val fromWeightKg: Double,
    val state: ProgressionState,
)

/**
 * Auto-progression after a session (docs/BLUEPRINT.md §3.3.1). Only completed
 * WORKING sets count. Returns null when there's nothing to progress (manual
 * rule, or no completed working sets).
 *
 * - DOUBLE: all sets reach the top of the range (and RPE ≤ target + 1 when
 *   logged) → add the increment and restart at the bottom of the range.
 *   Otherwise aim for one more rep, unless a set fell below the range.
 * - LINEAR: all sets reach the bottom of the range → add the increment.
 * - A set below the range counts as a miss; after `deloadAfterFailures`
 *   misses in a row the weight drops by `deloadPercent`.
 */
fun applyProgression(
    prescription: Prescription,
    previous: ProgressionState?,
    sets: List<WorkoutSet>,
): ProgressionResult? {
    if (prescription.progressionRule == ProgressionRule.NONE) return null
    val working = sets.filter { it.completed && it.type == SetType.WORKING && it.reps != null }
    if (working.isEmpty()) return null

    val weight = working.maxOf { it.weightKg ?: 0.0 }
    val reps = working.map { it.reps!! }
    val failures = previous?.consecutiveFailures ?: 0
    val effortOk = prescription.targetRpe == null ||
        working.all { it.rpe == null || it.rpe!! <= prescription.targetRpe!! + 1.0 }
    val missed = reps.any { it < prescription.repMin }

    val succeeded = when (prescription.progressionRule) {
        ProgressionRule.DOUBLE -> reps.all { it >= prescription.repMax } && effortOk
        ProgressionRule.LINEAR -> !missed && effortOk
        ProgressionRule.NONE -> false
    }

    return when {
        succeeded -> ProgressionResult(
            ProgressionOutcome.INCREASE,
            weight,
            ProgressionState(weight + prescription.incrementKg, prescription.repMin, consecutiveFailures = 0),
        )
        missed && failures + 1 >= prescription.deloadAfterFailures -> {
            val step = if (prescription.incrementKg > 0) minOf(prescription.incrementKg, 2.5) else 0.5
            val deloaded = roundToStep(weight * (1 - prescription.deloadPercent), step)
            ProgressionResult(
                ProgressionOutcome.DELOAD,
                weight,
                ProgressionState(deloaded, prescription.repMin, consecutiveFailures = 0),
            )
        }
        missed -> ProgressionResult(
            ProgressionOutcome.MISSED,
            weight,
            ProgressionState(weight, prescription.repMin, consecutiveFailures = failures + 1),
        )
        else -> ProgressionResult(
            ProgressionOutcome.HOLD,
            weight,
            ProgressionState(
                weight,
                nextTargetReps = (reps.min() + 1).coerceIn(prescription.repMin, prescription.repMax),
                consecutiveFailures = 0,
            ),
        )
    }
}

/** Training volume: weight × reps over completed, non-warm-up sets. */
fun volumeKg(sets: List<WorkoutSet>): Double =
    sets.filter { it.completed && it.type != SetType.WARMUP }
        .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
