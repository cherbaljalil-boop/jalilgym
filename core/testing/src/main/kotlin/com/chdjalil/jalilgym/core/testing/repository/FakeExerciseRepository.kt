package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.ExerciseDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeExerciseRepository(initial: List<Exercise> = emptyList()) : ExerciseRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = 1

    /** Drafts passed to [saveCustomExercise], in order. */
    val savedDrafts = mutableListOf<ExerciseDraft>()

    val current: List<Exercise> get() = state.value

    fun emit(exercises: List<Exercise>) {
        state.value = exercises
    }

    override fun observeExercises(): Flow<List<Exercise>> = state

    override fun observeExercise(id: String): Flow<Exercise?> = state.map { list -> list.find { it.id == id } }

    override fun observeExerciseCount(): Flow<Int> = state.map { it.size }

    override suspend fun saveCustomExercise(draft: ExerciseDraft): String {
        savedDrafts += draft
        val id = draft.id ?: "custom-${nextId++}"
        val exercise = Exercise(
            id = id,
            name = draft.name.trim(),
            aliases = emptyList(),
            instructions = draft.instructions,
            equipment = draft.equipment,
            mechanics = draft.mechanics,
            force = draft.force,
            difficulty = draft.difficulty,
            trackingType = draft.trackingType,
            defaultIncrementKg = 2.5,
            primaryMuscles = draft.primaryMuscles.sorted(),
            secondaryMuscles = draft.secondaryMuscles.sorted(),
            isBuiltIn = false,
            images = listOfNotNull(draft.imagePath),
        )
        state.update { list -> list.filterNot { it.id == id } + exercise }
        return id
    }

    override suspend fun deleteCustomExercise(id: String) {
        state.update { list -> list.filterNot { it.id == id } }
    }
}
