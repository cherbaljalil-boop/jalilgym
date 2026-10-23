package com.chdjalil.jalilgym.feature.today

import app.cash.turbine.test
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository()

    @Test
    fun `exercise count follows the library`() = runTest {
        val viewModel = TodayViewModel(exercises)

        viewModel.uiState.test {
            assertThat(expectMostRecentItem().exerciseCount).isEqualTo(0)

            exercises.emit(List(18) { sampleExercise("e$it") })

            assertThat(awaitItem().exerciseCount).isEqualTo(18)
        }
    }
}
