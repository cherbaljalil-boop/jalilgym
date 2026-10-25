package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.ExerciseDraft
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>

    fun observeExercise(id: String): Flow<Exercise?>

    fun observeExerciseCount(): Flow<Int>

    /** Creates or updates a custom exercise and returns its id. Built-in exercises can't be edited. */
    suspend fun saveCustomExercise(draft: ExerciseDraft): String

    /** Soft-deletes a custom exercise so logged history keeps pointing at it. */
    suspend fun deleteCustomExercise(id: String)
}
