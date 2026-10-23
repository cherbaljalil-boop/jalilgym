package com.chdjalil.jalilgym.feature.exercises.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.feature.exercises.navigation.EXERCISE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState

    data object NotFound : ExerciseDetailUiState

    data class Success(val exercise: Exercise) : ExerciseDetailUiState
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle[EXERCISE_ID_ARG])

    val uiState: StateFlow<ExerciseDetailUiState> = exerciseRepository.observeExercise(exerciseId)
        .map { exercise ->
            // A soft-deleted exercise only lingers here while we navigate away.
            if (exercise == null) ExerciseDetailUiState.NotFound else ExerciseDetailUiState.Success(exercise)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState.Loading)

    private val _deleted = MutableStateFlow(false)

    /** Becomes true once the exercise was deleted; the screen then navigates back. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun deleteExercise() {
        viewModelScope.launch {
            exerciseRepository.deleteCustomExercise(exerciseId)
            _deleted.value = true
        }
    }
}
