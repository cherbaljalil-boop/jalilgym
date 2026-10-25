package com.chdjalil.jalilgym.core.domain.progress

import com.chdjalil.jalilgym.core.domain.workout.ONE_REP_MAX_RELIABLE_REPS
import com.chdjalil.jalilgym.core.domain.workout.estimateOneRepMax
import com.chdjalil.jalilgym.core.model.PrType
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutSet

/** A candidate record from one exercise in one session. */
data class RecordCandidate(
    val type: PrType,
    val value: Double,
    /** The set that set it; null for session volume. */
    val set: WorkoutSet?,
)

/** Sets that count toward records: completed, not warm-ups, with load and reps. */
private fun WorkoutSet.counts() =
    completed && type != SetType.WARMUP && (weightKg ?: 0.0) > 0.0 && (reps ?: 0) > 0

/**
 * Best estimated 1RM (from sets of at most 12 reps), heaviest weight and total
 * volume for one exercise's sets in a session (docs/BLUEPRINT.md §3.5).
 */
fun sessionBests(sets: List<WorkoutSet>): List<RecordCandidate> {
    val counting = sets.filter { it.counts() }
    if (counting.isEmpty()) return emptyList()
    val result = mutableListOf<RecordCandidate>()
    counting.filter { it.reps!! <= ONE_REP_MAX_RELIABLE_REPS }
        .mapNotNull { set -> estimateOneRepMax(set.weightKg!!, set.reps!!)?.let { set to it } }
        .maxByOrNull { it.second }
        ?.let { (set, e1rm) -> result += RecordCandidate(PrType.EST_1RM, e1rm, set) }
    counting.maxWith(compareBy<WorkoutSet> { it.weightKg!! }.thenBy { it.reps!! })
        .let { result += RecordCandidate(PrType.MAX_WEIGHT, it.weightKg!!, it) }
    result += RecordCandidate(PrType.MAX_VOLUME, counting.sumOf { it.weightKg!! * it.reps!! }, null)
    return result
}

/**
 * Which candidates beat the current records ([previous], by type). A type with
 * no record yet is a baseline: it's stored but not celebrated.
 */
fun newRecords(candidates: List<RecordCandidate>, previous: Map<PrType, Double>): List<Pair<RecordCandidate, Double?>> =
    candidates.mapNotNull { candidate ->
        val best = previous[candidate.type]
        when {
            best == null -> candidate to null
            candidate.value > best + 1e-9 -> candidate to best
            else -> null
        }
    }
