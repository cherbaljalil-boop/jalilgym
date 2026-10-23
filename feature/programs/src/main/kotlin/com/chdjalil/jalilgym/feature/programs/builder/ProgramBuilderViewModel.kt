package com.chdjalil.jalilgym.feature.programs.builder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.feature.programs.navigation.PROGRAM_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProgramBuilderUiState {
    data object Loading : ProgramBuilderUiState

    data object NotFound : ProgramBuilderUiState

    data class Ready(val program: Program, val isActive: Boolean) : ProgramBuilderUiState
}

sealed interface ProgramBuilderEvent {
    data class Rename(val name: String) : ProgramBuilderEvent

    data class SetDaysPerWeek(val daysPerWeek: Int) : ProgramBuilderEvent

    data class AddDay(val name: String) : ProgramBuilderEvent

    data class RenameDay(val dayId: String, val name: String) : ProgramBuilderEvent

    data class DuplicateDay(val dayId: String) : ProgramBuilderEvent

    data class DeleteDay(val dayId: String) : ProgramBuilderEvent

    data class MoveDay(val dayId: String, val offset: Int) : ProgramBuilderEvent

    data object Activate : ProgramBuilderEvent

    data class Duplicate(val name: String) : ProgramBuilderEvent

    data object Delete : ProgramBuilderEvent
}

@HiltViewModel
class ProgramBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val programId: String = checkNotNull(savedStateHandle[PROGRAM_ID_ARG])

    val uiState: StateFlow<ProgramBuilderUiState> = combine(
        programRepository.observeProgram(programId),
        programRepository.observeActiveProgram(),
    ) { program, active ->
        if (program == null) {
            ProgramBuilderUiState.NotFound
        } else {
            ProgramBuilderUiState.Ready(program, isActive = active?.program?.id == program.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramBuilderUiState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _duplicatedProgramId = MutableStateFlow<String?>(null)
    val duplicatedProgramId: StateFlow<String?> = _duplicatedProgramId.asStateFlow()

    fun onEvent(event: ProgramBuilderEvent) {
        viewModelScope.launch {
            when (event) {
                is ProgramBuilderEvent.Rename ->
                    event.name.trim().takeIf { it.isNotEmpty() }?.let { programRepository.renameProgram(programId, it) }
                is ProgramBuilderEvent.SetDaysPerWeek -> programRepository.setDaysPerWeek(programId, event.daysPerWeek)
                is ProgramBuilderEvent.AddDay -> programRepository.addDay(programId, event.name.trim())
                is ProgramBuilderEvent.RenameDay ->
                    event.name.trim().takeIf { it.isNotEmpty() }?.let { programRepository.renameDay(event.dayId, it) }
                is ProgramBuilderEvent.DuplicateDay -> programRepository.duplicateDay(event.dayId)
                is ProgramBuilderEvent.DeleteDay -> programRepository.deleteDay(event.dayId)
                is ProgramBuilderEvent.MoveDay -> programRepository.moveDay(event.dayId, event.offset)
                ProgramBuilderEvent.Activate -> programRepository.activate(programId)
                is ProgramBuilderEvent.Duplicate ->
                    _duplicatedProgramId.value = programRepository.copyProgram(programId, event.name)
                ProgramBuilderEvent.Delete -> {
                    programRepository.deleteProgram(programId)
                    _deleted.value = true
                }
            }
        }
    }
}
