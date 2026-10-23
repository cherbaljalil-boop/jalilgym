package com.chdjalil.jalilgym.feature.exercises.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.PersonalRecord
import com.chdjalil.jalilgym.core.model.PrType
import com.chdjalil.jalilgym.core.model.StrengthPoint
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.exercises.navigation.EXERCISE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState

    data object NotFound : ExerciseDetailUiState

    data class Success(val exercise: Exercise) : ExerciseDetailUiState
}

/** The user's own numbers for this exercise. */
data class ExerciseHistoryUiState(
    val units: UnitSystem = UnitSystem.METRIC,
    /** Finished sessions, oldest first. */
    val sessions: List<StrengthPoint> = emptyList(),
    /** The current best of each record type. */
    val bests: List<PersonalRecord> = emptyList(),
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    progressRepository: ProgressRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle[EXERCISE_ID_ARG])

    val uiState: StateFlow<ExerciseDetailUiState> = exerciseRepository.observeExercise(exerciseId)
        .map { exercise ->
            // A soft-deleted exercise only lingers here while we navigate away.
            if (exercise == null) ExerciseDetailUiState.NotFound else ExerciseDetailUiState.Success(exercise)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState.Loading)

    val history: StateFlow<ExerciseHistoryUiState> = combine(
        preferencesRepository.userPreferences,
        progressRepository.observeStrengthSeries(exerciseId),
        progressRepository.observeRecords(exerciseId),
    ) { prefs, sessions, records ->
        val bests = PrType.entries.mapNotNull { type -> records.filter { it.type == type }.maxByOrNull { it.value } }
        ExerciseHistoryUiState(prefs.units, sessions, bests)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseHistoryUiState())

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
