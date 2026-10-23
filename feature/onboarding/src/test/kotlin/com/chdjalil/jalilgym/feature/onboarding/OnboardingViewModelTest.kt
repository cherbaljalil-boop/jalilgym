package com.chdjalil.jalilgym.feature.onboarding

import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()

    @Test
    fun `walks through the questions and saves the answers`() = runTest {
        val viewModel = OnboardingViewModel(preferences)

        viewModel.onEvent(OnboardingEvent.Continue)
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.GOAL)

        // Can't continue without an answer.
        viewModel.onEvent(OnboardingEvent.Continue)
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.GOAL)

        viewModel.onEvent(OnboardingEvent.GoalSelected(Goal.BUILD_MUSCLE))
        viewModel.onEvent(OnboardingEvent.Continue)
        viewModel.onEvent(OnboardingEvent.ExperienceSelected(ExperienceLevel.INTERMEDIATE))
        viewModel.onEvent(OnboardingEvent.Continue)

        val atProgram = viewModel.uiState.value
        assertThat(atProgram.step).isEqualTo(OnboardingStep.PROGRAM)
        assertThat(atProgram.program).isEqualTo(StarterProgram.PPL_6)

        viewModel.onEvent(OnboardingEvent.ProgramSelected(StarterProgram.PPL_3))
        viewModel.onEvent(OnboardingEvent.Continue)

        assertThat(viewModel.uiState.value.finished).isTrue()
        with(preferences.current) {
            assertThat(onboardingComplete).isTrue()
            assertThat(goal).isEqualTo(Goal.BUILD_MUSCLE)
            assertThat(experience).isEqualTo(ExperienceLevel.INTERMEDIATE)
            assertThat(starterProgram).isEqualTo(StarterProgram.PPL_3)
        }
    }

    @Test
    fun `back returns to the previous step and stops at welcome`() = runTest {
        val viewModel = OnboardingViewModel(preferences)
        viewModel.onEvent(OnboardingEvent.Continue)
        viewModel.onEvent(OnboardingEvent.GoalSelected(Goal.LOSE_FAT))
        viewModel.onEvent(OnboardingEvent.Continue)

        viewModel.onEvent(OnboardingEvent.Back)
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.GOAL)
        assertThat(viewModel.uiState.value.goal).isEqualTo(Goal.LOSE_FAT)

        viewModel.onEvent(OnboardingEvent.Back)
        viewModel.onEvent(OnboardingEvent.Back)
        assertThat(viewModel.uiState.value.step).isEqualTo(OnboardingStep.WELCOME)
    }

    @Test
    fun `skip completes onboarding without a program`() = runTest {
        val viewModel = OnboardingViewModel(preferences)
        viewModel.onEvent(OnboardingEvent.Continue)
        viewModel.onEvent(OnboardingEvent.GoalSelected(Goal.ENDURANCE))

        viewModel.onEvent(OnboardingEvent.Skip)

        assertThat(viewModel.uiState.value.finished).isTrue()
        assertThat(preferences.current.onboardingComplete).isTrue()
        assertThat(preferences.current.goal).isEqualTo(Goal.ENDURANCE)
        assertThat(preferences.current.starterProgram).isNull()
    }
}
