package com.chdjalil.jalilgym.core.domain.exercise

import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle

/**
 * Library filter. Empty sets mean "any". Within a group the options are OR'ed
 * (chest *or* triceps); across groups they are AND'ed (chest *and* dumbbell).
 */
data class ExerciseFilter(
    val query: String = "",
    val muscles: Set<Muscle> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
    val mechanics: Set<Mechanics> = emptySet(),
    val difficulty: Set<Difficulty> = emptySet(),
    /** When true, a muscle filter also matches exercises that only train it secondarily. */
    val includeSecondary: Boolean = false,
) {
    val activeFilterCount: Int
        get() = listOf(muscles, equipment, mechanics, difficulty).count { it.isNotEmpty() }

    val isEmpty: Boolean get() = query.isBlank() && activeFilterCount == 0
}

fun List<Exercise>.filteredBy(filter: ExerciseFilter): List<Exercise> {
    if (filter.isEmpty) return this
    val tokens = filter.query.normalizedTokens()
    return filter { exercise ->
        val matchesQuery = tokens.isEmpty() || exercise.searchText().let { text -> tokens.all { it in text } }
        val muscles = if (filter.includeSecondary) {
            exercise.primaryMuscles + exercise.secondaryMuscles
        } else {
            exercise.primaryMuscles
        }
        matchesQuery &&
            (filter.muscles.isEmpty() || muscles.any { it in filter.muscles }) &&
            (filter.equipment.isEmpty() || exercise.equipment in filter.equipment) &&
            (filter.mechanics.isEmpty() || exercise.mechanics in filter.mechanics) &&
            (filter.difficulty.isEmpty() || exercise.difficulty in filter.difficulty)
    }
}

private fun Exercise.searchText(): String =
    (listOf(name) + aliases).joinToString(" ").normalize()

private fun String.normalizedTokens(): List<String> =
    normalize().split(' ').filter { it.isNotBlank() }

/** Lowercase, and treat punctuation such as "-" and "'" as word breaks ("pull-up" == "pull up"). */
private fun String.normalize(): String =
    lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
