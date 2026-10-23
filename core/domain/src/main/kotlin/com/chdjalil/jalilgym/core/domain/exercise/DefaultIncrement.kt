package com.chdjalil.jalilgym.core.domain.exercise

import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrackingType

private val LOWER_BODY = setOf(Muscle.QUADS, Muscle.GLUTES, Muscle.HAMSTRINGS, Muscle.ADDUCTORS, Muscle.CALVES)

/**
 * Weight jump used by auto-progression when the user hasn't set one
 * (docs/BLUEPRINT.md §3.3.1): bigger for lower-body compounds, smaller for
 * dumbbells and isolation work.
 */
fun defaultIncrementKg(
    equipment: Equipment,
    mechanics: Mechanics,
    primaryMuscles: Collection<Muscle>,
    trackingType: TrackingType,
): Double {
    if (trackingType == TrackingType.TIME || trackingType == TrackingType.DISTANCE) return 0.0
    val lowerBody = primaryMuscles.any { it in LOWER_BODY }
    return when (equipment) {
        Equipment.DUMBBELL, Equipment.KETTLEBELL -> if (mechanics == Mechanics.COMPOUND) 2.0 else 1.0
        Equipment.MACHINE, Equipment.CABLE -> if (lowerBody) 5.0 else 2.5
        Equipment.BARBELL, Equipment.EZ_BAR, Equipment.SMITH_MACHINE, Equipment.OTHER ->
            if (lowerBody && mechanics == Mechanics.COMPOUND) 5.0 else 2.5
        Equipment.BODYWEIGHT, Equipment.BANDS -> 2.5
    }
}
