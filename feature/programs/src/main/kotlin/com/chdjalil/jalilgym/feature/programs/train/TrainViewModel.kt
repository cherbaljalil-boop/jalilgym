package com.chdjalil.jalilgym.feature.programs.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.Program
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainUiState(
    val isLoading: Boolean = true,
    val active: ActiveProgram? = null,
    val programs: List<Program> = emptyList(),
)

@HiltViewModel
class TrainViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val uiState: StateFlow<TrainUiState> = combine(
        programRepository.observeActiveProgram(),
        programRepository.observeUserPrograms(),
    ) { active, programs -> TrainUiState(isLoading = false, active = active, programs = programs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainUiState())

    private val _createdProgramId = MutableStateFlow<String?>(null)

    /** Set after "new blank program"; the screen navigates to it and calls [onNavigatedToCreated]. */
    val createdProgramId: StateFlow<String?> = _createdProgramId.asStateFlow()

    fun createBlankProgram(name: String) {
        viewModelScope.launch { _createdProgramId.value = programRepository.createProgram(name) }
    }

    fun onNavigatedToCreated() {
        _createdProgramId.value = null
    }

    private val _workoutStarted = MutableStateFlow(false)

    /** True once an empty workout was started (or an existing one resumed). */
    val workoutStarted: StateFlow<Boolean> = _workoutStarted.asStateFlow()

    fun startEmptyWorkout(name: String) {
        viewModelScope.launch {
            workoutRepository.startEmpty(name)
            _workoutStarted.value = true
        }
    }

    fun onWorkoutOpened() {
        _workoutStarted.value = false
    }
}
