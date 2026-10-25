package com.chdjalil.jalilgym.wear

import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.domain.units.toKg
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.wear.WearSet
import com.chdjalil.jalilgym.core.wear.WearSetCompletion
import kotlin.math.roundToInt

/**
 * The set being adjusted on the watch. Weight is in the user's unit so the
 * steps feel natural (2.5 kg or 5 lb); it goes back to kg when sent.
 */
data class SetDraft(
    val set: WearSet,
    val units: UnitSystem,
    /** In [units]; null for exercises not tracked by weight. */
    val weight: Double?,
    /** Reps, or seconds / meters. */
    val reps: Int,
) {
    val tracksWeight: Boolean get() = set.tracking == TrackingType.WEIGHT_REPS

    private val weightStep: Double get() = if (units == UnitSystem.IMPERIAL) 5.0 else 2.5

    private val repsStep: Int
        get() = when (set.tracking) {
            TrackingType.TIME -> 5
            TrackingType.DISTANCE -> 10
            else -> 1
        }

    fun changeWeight(steps: Int): SetDraft {
        if (!tracksWeight) return this
        val next = ((weight ?: 0.0) + steps * weightStep).coerceAtLeast(0.0)
        return copy(weight = roundToTenth(next))
    }

    fun changeReps(steps: Int): SetDraft = copy(reps = (reps + steps * repsStep).coerceAtLeast(0))

    /** What to send to the phone; an untouched weight goes back exactly as it came. */
    fun completion(): WearSetCompletion {
        val kg = when {
            !tracksWeight || weight == null -> null
            weight == from(set, units).weight -> set.weightKg ?: units.toKg(weight)
            else -> units.toKg(weight)
        }
        return WearSetCompletion(set.setId, kg, reps)
    }

    companion object {
        fun from(set: WearSet, units: UnitSystem) = SetDraft(
            set = set,
            units = units,
            weight = if (set.tracking == TrackingType.WEIGHT_REPS) roundToTenth(units.fromKg(set.weightKg ?: 0.0)) else null,
            reps = set.reps ?: 0,
        )

        private fun roundToTenth(value: Double) = (value * 10).roundToInt() / 10.0
    }
}
