package com.chdjalil.jalilgym.feature.exercises.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseFilter
import com.chdjalil.jalilgym.core.domain.exercise.filteredBy
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ExerciseLibraryUiState(
    val isLoading: Boolean = true,
    val filter: ExerciseFilter = ExerciseFilter(),
    val exercises: List<Exercise> = emptyList(),
    val totalCount: Int = 0,
)

sealed interface ExerciseLibraryEvent {
    data class QueryChanged(val query: String) : ExerciseLibraryEvent

    data class MuscleToggled(val muscle: Muscle) : ExerciseLibraryEvent

    data class EquipmentToggled(val equipment: Equipment) : ExerciseLibraryEvent

    data class MechanicsToggled(val mechanics: Mechanics) : ExerciseLibraryEvent

    data class DifficultyToggled(val difficulty: Difficulty) : ExerciseLibraryEvent

    data class IncludeSecondaryChanged(val include: Boolean) : ExerciseLibraryEvent

    data object FiltersCleared : ExerciseLibraryEvent
}

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ExerciseFilter())

    val uiState: StateFlow<ExerciseLibraryUiState> =
        combine(exerciseRepository.observeExercises(), filter) { exercises, filter ->
            ExerciseLibraryUiState(
                isLoading = false,
                filter = filter,
                exercises = exercises.filteredBy(filter),
                totalCount = exercises.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    fun onEvent(event: ExerciseLibraryEvent) {
        filter.update { current ->
            when (event) {
                is ExerciseLibraryEvent.QueryChanged -> current.copy(query = event.query)
                is ExerciseLibraryEvent.MuscleToggled -> current.copy(muscles = current.muscles.toggle(event.muscle))
                is ExerciseLibraryEvent.EquipmentToggled ->
                    current.copy(equipment = current.equipment.toggle(event.equipment))
                is ExerciseLibraryEvent.MechanicsToggled ->
                    current.copy(mechanics = current.mechanics.toggle(event.mechanics))
                is ExerciseLibraryEvent.DifficultyToggled ->
                    current.copy(difficulty = current.difficulty.toggle(event.difficulty))
                is ExerciseLibraryEvent.IncludeSecondaryChanged -> current.copy(includeSecondary = event.include)
                ExerciseLibraryEvent.FiltersCleared -> ExerciseFilter(query = current.query)
            }
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
