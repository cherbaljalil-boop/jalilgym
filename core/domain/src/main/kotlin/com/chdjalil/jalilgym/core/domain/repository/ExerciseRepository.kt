package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>

    fun observeExerciseCount(): Flow<Int>
}
