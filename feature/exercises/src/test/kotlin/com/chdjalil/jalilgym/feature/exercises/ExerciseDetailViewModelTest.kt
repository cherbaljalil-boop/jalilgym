package com.chdjalil.jalilgym.feature.exercises

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeProgressRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.feature.exercises.detail.ExerciseDetailUiState
import com.chdjalil.jalilgym.feature.exercises.detail.ExerciseDetailViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExerciseDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository(listOf(sampleExercise("bench", "Bench Press")))
    private val progress = FakeProgressRepository()

    private fun viewModel(id: String) = ExerciseDetailViewModel(
        SavedStateHandle(mapOf("exerciseId" to id)),
        repository,
        progress,
        FakeUserPreferencesRepository(),
    )

    @Test
    fun `loads the exercise or reports it missing`() = runTest {
        val found = viewModel("bench")
        val missing = viewModel("nope")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { found.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { missing.uiState.collect {} }

        assertThat((found.uiState.value as ExerciseDetailUiState.Success).exercise.name).isEqualTo("Bench Press")
        assertThat(missing.uiState.value).isEqualTo(ExerciseDetailUiState.NotFound)
    }

    @Test
    fun `delete removes the exercise and signals navigation`() = runTest {
        val viewModel = viewModel("bench")

        viewModel.deleteExercise()

        assertThat(repository.current).isEmpty()
        assertThat(viewModel.deleted.value).isTrue()
    }
}
