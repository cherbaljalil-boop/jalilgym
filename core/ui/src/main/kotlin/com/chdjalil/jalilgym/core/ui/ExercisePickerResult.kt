package com.chdjalil.jalilgym.core.ui

/**
 * Contract between the exercise picker (feature:exercises) and screens that ask
 * for exercises: the picker writes the chosen ids to the *previous* back stack
 * entry's SavedStateHandle under [KEY] and pops itself.
 */
object ExercisePickerResult {
    const val KEY = "picked_exercise_ids"
}
