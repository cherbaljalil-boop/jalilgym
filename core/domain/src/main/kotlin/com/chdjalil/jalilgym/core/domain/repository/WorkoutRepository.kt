package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutHistoryItem
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow

/**
 * Workout sessions. Every change is written to the database immediately, so an
 * in-progress workout survives process death and is resumed on next launch.
 */
interface WorkoutRepository {
    /** The workout in progress, if any (at most one at a time). */
    fun observeActiveSession(): Flow<WorkoutSession?>

    fun observeSession(id: String): Flow<WorkoutSession?>

    /** Finished workouts, newest first. */
    fun observeHistory(): Flow<List<WorkoutHistoryItem>>

    /**
     * Starts a workout from a program day, pre-filling each working set from the
     * progression target or, failing that, the last time the exercise was done.
     */
    suspend fun startFromDay(dayId: String): String

    suspend fun startEmpty(name: String): String

    suspend fun addExercises(sessionId: String, exerciseIds: List<String>)

    /** Replaces the exercise but keeps the logged sets. */
    suspend fun swapExercise(sessionExerciseId: String, exerciseId: String)

    suspend fun removeExercise(sessionExerciseId: String)

    /** Adds a set after the last one, copying its weight and reps. */
    suspend fun addSet(sessionExerciseId: String, type: SetType = SetType.WORKING): String

    suspend fun removeSet(setId: String)

    /**
     * Changes a set's weight, reps, effort or type. [transform] is applied to the
     * latest stored values, one edit at a time, so quick successive edits
     * (weight, then reps) never overwrite each other. Completion is set via [setCompleted].
     */
    suspend fun editSet(setId: String, transform: (WorkoutSet) -> WorkoutSet)

    suspend fun setCompleted(setId: String, completed: Boolean)

    /** Inserts warm-up sets (weight in kg, reps) before the existing sets. */
    suspend fun addWarmups(sessionExerciseId: String, warmups: List<Pair<Double, Int>>)

    /** Ends the workout, applies auto-progression and moves the program rotation on. */
    suspend fun finish(sessionId: String, notes: String?): WorkoutSummary

    /** Throws away an unfinished workout. */
    suspend fun discard(sessionId: String)

    suspend fun deleteSession(id: String)

    /**
     * Summary of a finished workout. Progression notes are only available for
     * workouts finished during this app session.
     */
    fun observeSummary(sessionId: String): Flow<WorkoutSummary?>
}
