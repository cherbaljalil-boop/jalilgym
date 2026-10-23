package com.chdjalil.jalilgym.core.model

import java.time.LocalDate

data class PersonalRecord(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val type: PrType,
    /** kg for weights and volume, estimated kg for 1RM. */
    val value: Double,
    val achievedAt: Long,
    /** The set behind the record, when it's a single set (not volume). */
    val weightKg: Double?,
    val reps: Int?,
)

/** A record set in a finished workout; [previousValue] is null for a first-ever baseline. */
data class RecordNote(
    val exerciseName: String,
    val type: PrType,
    val value: Double,
    val previousValue: Double?,
) {
    val isImprovement: Boolean get() = previousValue != null && value > previousValue
}

/** One finished session of an exercise, for strength charts. */
data class StrengthPoint(
    val sessionId: String,
    val performedAt: Long,
    val bestEstimatedOneRepMaxKg: Double?,
    val topWeightKg: Double?,
    val volumeKg: Double,
)

data class ExerciseProgress(
    val exercise: Exercise,
    val sessionCount: Int,
    val lastPerformedAt: Long,
    val bestEstimatedOneRepMaxKg: Double?,
)

data class TrainingStreak(
    /** Consecutive weeks on target, including this week once it's on target. */
    val currentWeeks: Int,
    val bestWeeks: Int,
    val thisWeekWorkouts: Int,
    val weeklyTarget: Int,
    val lastWorkoutDate: LocalDate?,
)

data class BodyWeight(val id: String, val date: LocalDate, val weightKg: Double)

data class BodyMeasurement(val id: String, val date: LocalDate, val type: MeasurementType, val value: Double)

data class ProgressPhoto(
    val id: String,
    val date: LocalDate,
    val pose: PhotoPose,
    val filePath: String,
    val weightKgAtTime: Double?,
)

enum class AchievementCategory { MILESTONES, STREAKS, STRENGTH, VOLUME, NUTRITION, EXPLORER }

/** A badge with how close the user is to it. */
data class AchievementStatus(
    val id: String,
    val category: AchievementCategory,
    val target: Double,
    val progress: Double,
    val unlockedAt: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val fraction: Float get() = if (target <= 0) 1f else (progress / target).coerceIn(0.0, 1.0).toFloat()
}
