package com.chdjalil.jalilgym.core.model

/** A logged (or in-progress) workout. */
data class WorkoutSession(
    val id: String,
    val name: String,
    val programDayId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val status: SessionStatus,
    val notes: String?,
    val exercises: List<SessionExercise>,
) {
    val isActive: Boolean get() = status == SessionStatus.ACTIVE
}

data class SessionExercise(
    val id: String,
    val exercise: Exercise,
    val position: Int,
    val supersetGroup: String?,
    val programExerciseId: String?,
    /** Current program targets for this slot; null for exercises added ad hoc. */
    val target: Prescription?,
    val sets: List<WorkoutSet>,
    /** Sets from the last finished session with this exercise, shown as "previous". */
    val previous: List<WorkoutSet> = emptyList(),
)

/**
 * One set. Weight is stored in kg (null = not entered yet; 0 = bodyweight/unloaded).
 * [reps] holds seconds for timed exercises and meters for distance work.
 */
data class WorkoutSet(
    val id: String,
    val position: Int,
    val type: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val rir: Int?,
    val completed: Boolean,
    val completedAt: Long?,
)

/** What changed for an exercise's next session after a workout was finished. */
data class ProgressionNote(
    val exerciseName: String,
    val outcome: ProgressionOutcome,
    val fromWeightKg: Double,
    val toWeightKg: Double,
    val nextTargetReps: Int,
)

enum class ProgressionOutcome { INCREASE, HOLD, MISSED, DELOAD }

data class WorkoutSummary(
    val session: WorkoutSession,
    val durationMillis: Long,
    val totalVolumeKg: Double,
    val completedSets: Int,
    val progression: List<ProgressionNote>,
)

/** Finished session as shown in history lists. */
data class WorkoutHistoryItem(
    val id: String,
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val totalVolumeKg: Double,
    val exerciseCount: Int,
    val completedSets: Int,
)
