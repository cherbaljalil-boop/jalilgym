package com.chdjalil.jalilgym.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.onboarding.suggestStarterProgram
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, GOAL, EXPERIENCE, PROGRAM }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val goal: Goal? = null,
    val experience: ExperienceLevel? = null,
    /** The program the user will start with; defaults to the suggestion. */
    val program: StarterProgram? = null,
    val isSaving: Boolean = false,
    val finished: Boolean = false,
) {
    val suggestedProgram: StarterProgram get() = suggestStarterProgram(goal, experience)

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.GOAL -> goal != null
            OnboardingStep.EXPERIENCE -> experience != null
            OnboardingStep.PROGRAM -> program != null
        }

    /** 1-based question number for the progress indicator; the welcome page isn't counted. */
    val questionNumber: Int get() = step.ordinal
}

sealed interface OnboardingEvent {
    data class GoalSelected(val goal: Goal) : OnboardingEvent

    data class ExperienceSelected(val experience: ExperienceLevel) : OnboardingEvent

    data class ProgramSelected(val program: StarterProgram) : OnboardingEvent

    data object Continue : OnboardingEvent

    data object Back : OnboardingEvent

    /** Finish now with whatever has been answered; no starter program is chosen. */
    data object Skip : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.GoalSelected -> _uiState.update { it.copy(goal = event.goal) }
            is OnboardingEvent.ExperienceSelected -> _uiState.update { it.copy(experience = event.experience) }
            is OnboardingEvent.ProgramSelected -> _uiState.update { it.copy(program = event.program) }
            OnboardingEvent.Continue -> onContinue()
            OnboardingEvent.Back -> _uiState.update { state ->
                val previous = OnboardingStep.entries.getOrNull(state.step.ordinal - 1) ?: return@update state
                state.copy(step = previous)
            }
            OnboardingEvent.Skip -> finish(program = null)
        }
    }

    private fun onContinue() {
        val state = _uiState.value
        if (!state.canContinue) return
        when (state.step) {
            OnboardingStep.WELCOME -> _uiState.update { it.copy(step = OnboardingStep.GOAL) }
            OnboardingStep.GOAL -> _uiState.update { it.copy(step = OnboardingStep.EXPERIENCE) }
            // Pre-select the suggestion each time answers lead to the last step.
            OnboardingStep.EXPERIENCE -> _uiState.update {
                it.copy(step = OnboardingStep.PROGRAM, program = it.suggestedProgram)
            }
            OnboardingStep.PROGRAM -> finish(state.program)
        }
    }

    private fun finish(program: StarterProgram?) {
        val state = _uiState.value
        if (state.isSaving || state.finished) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            preferencesRepository.completeOnboarding(state.goal, state.experience, program)
            _uiState.update { it.copy(isSaving = false, finished = true) }
        }
    }
}
