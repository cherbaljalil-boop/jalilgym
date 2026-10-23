package com.chdjalil.jalilgym.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.StarterProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TodayUiState(
    val exerciseCount: Int? = null,
    /** Picked during onboarding; becomes a real program in Phase 2. */
    val starterProgram: StarterProgram? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        exerciseRepository.observeExerciseCount(),
        preferencesRepository.userPreferences,
    ) { count, preferences ->
        TodayUiState(exerciseCount = count, starterProgram = preferences.starterProgram)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
}
