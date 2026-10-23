package com.chdjalil.jalilgym.core.domain.program

import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.TrackingType

/**
 * Starting prescription when an exercise is added to a program day, based on the
 * user's goal (docs/BLUEPRINT.md §3.3): compounds get lower reps and longer rest.
 */
fun defaultPrescription(
    exercise: Exercise,
    goal: Goal?,
    restCompoundSec: Int,
    restIsolationSec: Int,
): Prescription {
    val compound = exercise.mechanics == Mechanics.COMPOUND
    val rest = if (compound) restCompoundSec else restIsolationSec
    return when (exercise.trackingType) {
        TrackingType.TIME -> Prescription(
            targetSets = 3, repMin = 30, repMax = 60, restSeconds = restIsolationSec,
            progressionRule = ProgressionRule.NONE, incrementKg = 0.0,
        )
        TrackingType.DISTANCE -> Prescription(
            targetSets = 3, repMin = 20, repMax = 40, restSeconds = rest,
            progressionRule = ProgressionRule.NONE, incrementKg = exercise.defaultIncrementKg,
        )
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val (min, max) = repRange(goal, compound)
            Prescription(
                targetSets = 3,
                repMin = min,
                repMax = max,
                restSeconds = rest,
                progressionRule = ProgressionRule.DOUBLE,
                incrementKg = exercise.defaultIncrementKg,
            )
        }
    }
}

private fun repRange(goal: Goal?, compound: Boolean): Pair<Int, Int> = when (goal) {
    Goal.GET_STRONGER -> if (compound) 4 to 6 else 8 to 12
    Goal.ENDURANCE -> if (compound) 12 to 15 else 15 to 20
    Goal.BUILD_MUSCLE, Goal.LOSE_FAT, Goal.GENERAL_FITNESS, null -> if (compound) 8 to 12 else 10 to 15
}
