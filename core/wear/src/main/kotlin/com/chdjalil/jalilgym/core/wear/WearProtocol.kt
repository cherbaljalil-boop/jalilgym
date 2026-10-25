package com.chdjalil.jalilgym.core.wear

import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import com.chdjalil.jalilgym.core.domain.workout.nextIncompleteSet
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.WorkoutSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the phone and the watch app say to each other over the Wearable Data
 * Layer. The phone owns the workout; the watch shows the next set and sends
 * back what was lifted.
 */
object WearPaths {
    /** Data item with the current [WearWorkoutState] as JSON under [STATE_KEY]; the watch reads it. */
    const val STATE = "/jalilgym/workout"
    const val STATE_KEY = "state"

    /** Message from the watch: a [WearSetCompletion]. */
    const val COMPLETE_SET = "/jalilgym/workout/complete-set"

    /** Message from the watch: skip the running rest timer (empty body). */
    const val SKIP_REST = "/jalilgym/workout/skip-rest"
}

@Serializable
data class WearWorkoutState(
    val active: Boolean = false,
    val workoutName: String? = null,
    /** Null once every set is done. */
    val next: WearSet? = null,
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    /** Weights are shown in this system; the protocol always carries kg. */
    val units: UnitSystem = UnitSystem.METRIC,
    /** Wall-clock end of the running rest timer, epoch millis. */
    val restEndsAt: Long? = null,
    val restLabel: String? = null,
)

@Serializable
data class WearSet(
    val setId: String,
    val exerciseName: String,
    /** 1-based position among the exercise's sets. */
    val number: Int,
    val count: Int,
    val tracking: TrackingType,
    val isWarmup: Boolean,
    /** Suggested values: what's entered, else last time's, else the program target. */
    val weightKg: Double?,
    /** Reps, or seconds / meters for timed and distance work. */
    val reps: Int?,
    val incrementKg: Double,
)

@Serializable
data class WearSetCompletion(val setId: String, val weightKg: Double?, val reps: Int?)

val WearJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** The state the watch should show for [session] (null = no workout open). */
fun wearWorkoutState(session: WorkoutSession?, rest: RestTimerState?, units: UnitSystem): WearWorkoutState {
    if (session == null || !session.isActive) return WearWorkoutState(units = units)
    val allSets = session.exercises.flatMap { it.sets }
    val next = session.nextIncompleteSet()?.let { (exercise, set) ->
        val ordered = exercise.sets.sortedBy { it.position }
        val index = ordered.indexOf(set)
        val previous = exercise.previous.sortedBy { it.position }.getOrNull(index)
        val doneBefore = ordered.take(index).lastOrNull { it.completed }
        WearSet(
            setId = set.id,
            exerciseName = exercise.exercise.name,
            number = index + 1,
            count = ordered.size,
            tracking = exercise.exercise.trackingType,
            isWarmup = set.type == SetType.WARMUP,
            weightKg = set.weightKg ?: doneBefore?.weightKg ?: previous?.weightKg,
            reps = set.reps ?: doneBefore?.reps ?: previous?.reps ?: exercise.target?.repMin,
            incrementKg = exercise.target?.incrementKg ?: exercise.exercise.defaultIncrementKg,
        )
    }
    return WearWorkoutState(
        active = true,
        workoutName = session.name,
        next = next,
        completedSets = allSets.count { it.completed },
        totalSets = allSets.size,
        units = units,
        restEndsAt = rest?.endsAtMillis,
        restLabel = rest?.label,
    )
}
