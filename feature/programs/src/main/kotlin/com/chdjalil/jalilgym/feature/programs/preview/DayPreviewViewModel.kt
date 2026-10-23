package com.chdjalil.jalilgym.feature.programs.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.feature.programs.navigation.DAY_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DayPreviewUiState {
    data object Loading : DayPreviewUiState

    data object NotFound : DayPreviewUiState

    data class Ready(
        val day: ProgramDay,
        val programName: String?,
        /** True when this day belongs to the active program but isn't the next one in rotation. */
        val canMakeNext: Boolean,
        val isNext: Boolean,
    ) : DayPreviewUiState
}

@HiltViewModel
class DayPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val dayId: String = checkNotNull(savedStateHandle[DAY_ID_ARG])

    val uiState: StateFlow<DayPreviewUiState> = combine(
        programRepository.observeDay(dayId),
        programRepository.observeActiveProgram(),
    ) { day, active ->
        if (day == null) {
            DayPreviewUiState.NotFound
        } else {
            val inActive = active?.program?.id == day.programId
            val isNext = inActive && active?.nextDay?.id == day.id
            DayPreviewUiState.Ready(
                day = day,
                programName = active?.program?.name?.takeIf { inActive },
                canMakeNext = inActive && !isNext,
                isNext = isNext,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayPreviewUiState.Loading)

    /** Train this day next instead of the one the rotation suggests. */
    fun makeNext() {
        val state = uiState.value as? DayPreviewUiState.Ready ?: return
        viewModelScope.launch { programRepository.setNextDay(state.day.dayIndex) }
    }
}
