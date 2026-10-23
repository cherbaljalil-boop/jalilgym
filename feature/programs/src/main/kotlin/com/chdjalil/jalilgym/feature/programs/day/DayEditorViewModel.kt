package com.chdjalil.jalilgym.feature.programs.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.ui.ExercisePickerResult
import com.chdjalil.jalilgym.feature.programs.navigation.DAY_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DayEditorUiState {
    data object Loading : DayEditorUiState

    data object NotFound : DayEditorUiState

    data class Ready(val day: ProgramDay) : DayEditorUiState
}

sealed interface DayEditorEvent {
    data class Rename(val name: String) : DayEditorEvent

    data class Move(val programExerciseId: String, val offset: Int) : DayEditorEvent

    data class Remove(val programExerciseId: String) : DayEditorEvent

    data class ToggleSupersetWithNext(val index: Int) : DayEditorEvent

    data class UpdatePrescription(val programExerciseId: String, val prescription: Prescription) : DayEditorEvent
}

@HiltViewModel
class DayEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val dayId: String = checkNotNull(savedStateHandle[DAY_ID_ARG])

    val uiState: StateFlow<DayEditorUiState> = programRepository.observeDay(dayId)
        .map { day -> if (day == null) DayEditorUiState.NotFound else DayEditorUiState.Ready(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditorUiState.Loading)

    init {
        // Exercises chosen in the picker arrive through this entry's SavedStateHandle.
        viewModelScope.launch {
            savedStateHandle.getStateFlow<List<String>?>(ExercisePickerResult.KEY, null)
                .filterNotNull()
                .collect { ids ->
                    savedStateHandle[ExercisePickerResult.KEY] = null
                    programRepository.addExercises(dayId, ids)
                }
        }
    }

    fun onEvent(event: DayEditorEvent) {
        viewModelScope.launch {
            when (event) {
                is DayEditorEvent.Rename ->
                    event.name.trim().takeIf { it.isNotEmpty() }?.let { programRepository.renameDay(dayId, it) }
                is DayEditorEvent.Move -> programRepository.moveExercise(event.programExerciseId, event.offset)
                is DayEditorEvent.Remove -> programRepository.removeExercise(event.programExerciseId)
                is DayEditorEvent.ToggleSupersetWithNext -> programRepository.toggleSupersetWithNext(dayId, event.index)
                is DayEditorEvent.UpdatePrescription ->
                    programRepository.updatePrescription(event.programExerciseId, event.prescription)
            }
        }
    }
}
