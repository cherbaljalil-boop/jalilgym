package com.chdjalil.jalilgym.feature.workout.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.WorkoutSummary
import com.chdjalil.jalilgym.feature.workout.navigation.SESSION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data object NotFound : SessionUiState

    data class Ready(val summary: WorkoutSummary, val units: UnitSystem) : SessionUiState
}

/** Backs both the post-workout summary and the history detail page. */
@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle[SESSION_ID_ARG])

    val uiState: StateFlow<SessionUiState> = combine(
        workoutRepository.observeSummary(sessionId),
        preferencesRepository.userPreferences,
    ) { summary, prefs ->
        if (summary == null) SessionUiState.NotFound else SessionUiState.Ready(summary, prefs.units)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState.Loading)

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
            onDeleted()
        }
    }
}
