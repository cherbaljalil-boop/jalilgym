package com.chdjalil.jalilgym.core.domain.exercise

import com.chdjalil.jalilgym.core.model.ExerciseDraft

enum class ExerciseDraftError { NAME_BLANK, NAME_TOO_LONG, NO_PRIMARY_MUSCLE, MUSCLE_IN_BOTH_ROLES }

const val EXERCISE_NAME_MAX_LENGTH = 60

fun ExerciseDraft.validate(): Set<ExerciseDraftError> = buildSet {
    if (name.isBlank()) add(ExerciseDraftError.NAME_BLANK)
    if (name.trim().length > EXERCISE_NAME_MAX_LENGTH) add(ExerciseDraftError.NAME_TOO_LONG)
    if (primaryMuscles.isEmpty()) add(ExerciseDraftError.NO_PRIMARY_MUSCLE)
    if (primaryMuscles.any { it in secondaryMuscles }) add(ExerciseDraftError.MUSCLE_IN_BOTH_ROLES)
}
