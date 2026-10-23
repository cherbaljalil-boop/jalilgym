package com.chdjalil.jalilgym.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.TrainingStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val isLoading: Boolean = true,
    val exerciseCount: Int? = null,
    val active: ActiveProgram? = null,
    /** Name of the workout in progress, if any. */
    val workoutInProgress: String? = null,
    val streak: TrainingStreak? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
    workoutRepository: WorkoutRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        exerciseRepository.observeExerciseCount(),
        programRepository.observeActiveProgram(),
        workoutRepository.observeActiveSession(),
        progressRepository.observeTrainingStreak(),
    ) { count, active, session, streak ->
        TodayUiState(isLoading = false, exerciseCount = count, active = active, workoutInProgress = session?.name, streak = streak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Skip today's planned workout and move on to the next one in the rotation. */
    fun skipToNextDay() {
        viewModelScope.launch { programRepository.advanceRotation() }
    }
}
