package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExerciseRepository(initial: List<Exercise> = emptyList()) : ExerciseRepository {
    private val state = MutableStateFlow(initial)

    fun emit(exercises: List<Exercise>) {
        state.value = exercises
    }

    override fun observeExercises(): Flow<List<Exercise>> = state

    override fun observeExerciseCount(): Flow<Int> = state.map { it.size }
}
