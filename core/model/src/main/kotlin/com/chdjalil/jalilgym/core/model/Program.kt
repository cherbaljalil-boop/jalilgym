package com.chdjalil.jalilgym.core.model

data class Program(
    val id: String,
    val name: String,
    val type: ProgramType,
    /** Target training days per week (used for scheduling and streaks); may differ from [days].size. */
    val daysPerWeek: Int,
    val isTemplate: Boolean,
    val sourceTemplateId: String?,
    /** Days in rotation order. */
    val days: List<ProgramDay>,
)

data class ProgramDay(
    val id: String,
    val programId: String,
    val dayIndex: Int,
    val name: String,
    /** Exercises in workout order. */
    val exercises: List<ProgramExercise>,
)

/**
 * One exercise slot in a program day. For [TrackingType.TIME] exercises the rep
 * range is in seconds; for [TrackingType.DISTANCE] it's in meters.
 */
data class ProgramExercise(
    val id: String,
    val dayId: String,
    val exercise: Exercise,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRpe: Double?,
    val restSeconds: Int,
    /** Consecutive exercises sharing a group are a superset; rest comes after the last one. */
    val supersetGroup: String?,
    val progressionRule: ProgressionRule,
    val incrementKg: Double,
    val deloadAfterFailures: Int,
    val deloadPercent: Double,
)

/** How a program exercise is prescribed; the editable part of [ProgramExercise]. */
data class Prescription(
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRpe: Double? = null,
    val restSeconds: Int,
    val progressionRule: ProgressionRule = ProgressionRule.DOUBLE,
    val incrementKg: Double,
    val deloadAfterFailures: Int = 2,
    val deloadPercent: Double = 0.10,
)

val ProgramExercise.prescription: Prescription
    get() = Prescription(
        targetSets = targetSets,
        repMin = repMin,
        repMax = repMax,
        targetRpe = targetRpe,
        restSeconds = restSeconds,
        progressionRule = progressionRule,
        incrementKg = incrementKg,
        deloadAfterFailures = deloadAfterFailures,
        deloadPercent = deloadPercent,
    )

/** The program being followed and the next day in its rotation. */
data class ActiveProgram(
    val program: Program,
    val nextDayIndex: Int,
    val startedAt: Long,
) {
    /** Null when the program has no days yet. */
    val nextDay: ProgramDay?
        get() = program.days.getOrNull(nextDayIndex.coerceIn(0, (program.days.size - 1).coerceAtLeast(0)))
}
