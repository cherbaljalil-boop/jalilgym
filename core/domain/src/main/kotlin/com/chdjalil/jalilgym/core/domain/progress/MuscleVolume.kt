package com.chdjalil.jalilgym.core.domain.progress

import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SetType

/** Weekly target band for sets per muscle (docs/BLUEPRINT.md §3.5). */
val WEEKLY_SET_TARGET = 10.0..20.0

/**
 * Working sets per muscle: a completed non-warm-up set counts 1 for each
 * primary muscle and 0.5 for each secondary one.
 */
fun muscleSets(exercises: List<SessionExercise>): Map<Muscle, Double> {
    val totals = mutableMapOf<Muscle, Double>()
    exercises.forEach { entry ->
        val sets = entry.sets.count { it.completed && it.type != SetType.WARMUP }
        if (sets == 0) return@forEach
        entry.exercise.primaryMuscles.forEach { totals.merge(it, sets.toDouble(), Double::plus) }
        entry.exercise.secondaryMuscles.forEach { totals.merge(it, sets * 0.5, Double::plus) }
    }
    return totals
}

/** 0..1 heat for the body map, relative to the top of the weekly target band. */
fun Map<Muscle, Double>.toHeat(): Map<Muscle, Float> =
    mapValues { (_, sets) -> (sets / WEEKLY_SET_TARGET.endInclusive).coerceIn(0.0, 1.0).toFloat() }
