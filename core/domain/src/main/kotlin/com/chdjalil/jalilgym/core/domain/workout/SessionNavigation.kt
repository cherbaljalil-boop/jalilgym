package com.chdjalil.jalilgym.core.domain.workout

import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet

/** The next set to do: the first incomplete set, going through exercises in order. */
fun WorkoutSession.nextIncompleteSet(): Pair<SessionExercise, WorkoutSet>? =
    exercises.sortedBy { it.position }.firstNotNullOfOrNull { exercise ->
        exercise.sets.sortedBy { it.position }.firstOrNull { !it.completed }?.let { exercise to it }
    }

/**
 * Rest to start after completing [set] of [exercise], or null for no timer.
 * Within a superset you move straight to the next exercise; rest comes after
 * the last one. Warm-up sets don't start a timer.
 */
fun WorkoutSession.restAfter(
    exercise: SessionExercise,
    set: WorkoutSet,
    defaultCompoundSec: Int,
    defaultIsolationSec: Int,
): Int? {
    if (set.type == SetType.WARMUP) return null
    val ordered = exercises.sortedBy { it.position }
    val index = ordered.indexOfFirst { it.id == exercise.id }
    val group = exercise.supersetGroup
    val next = ordered.getOrNull(index + 1)
    if (group != null && next != null && next.supersetGroup == group) return null
    val seconds = exercise.target?.restSeconds
        ?: if (exercise.exercise.mechanics == Mechanics.COMPOUND) defaultCompoundSec else defaultIsolationSec
    return seconds.takeIf { it > 0 }
}
