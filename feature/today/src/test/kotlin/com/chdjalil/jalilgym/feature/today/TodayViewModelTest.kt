package com.chdjalil.jalilgym.feature.today

import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository()
    private val preferences = FakeUserPreferencesRepository()

    @Test
    fun `follows the library size and the chosen starter program`() = runTest {
        val viewModel = TodayViewModel(exercises, preferences)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value).isEqualTo(TodayUiState(exerciseCount = 0, starterProgram = null))

        exercises.emit(List(18) { sampleExercise("e$it") })
        preferences.completeOnboarding(goal = null, experience = null, starterProgram = StarterProgram.PPL_3)

        assertThat(viewModel.uiState.value)
            .isEqualTo(TodayUiState(exerciseCount = 18, starterProgram = StarterProgram.PPL_3))
    }
}
