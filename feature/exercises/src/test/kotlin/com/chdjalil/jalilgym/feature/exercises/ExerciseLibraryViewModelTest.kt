package com.chdjalil.jalilgym.feature.exercises

import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryEvent
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExerciseLibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository(
        listOf(
            sampleExercise("bench", "Barbell Bench Press"),
            sampleExercise("curl", "Dumbbell Curl").copy(equipment = Equipment.DUMBBELL, primaryMuscles = listOf(Muscle.BICEPS)),
            sampleExercise("row", "Dumbbell Row").copy(equipment = Equipment.DUMBBELL, primaryMuscles = listOf(Muscle.LATS)),
        ),
    )

    @Test
    fun `shows all exercises, then narrows by query and filters`() = runTest {
        val viewModel = ExerciseLibraryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.exercises.map { it.id }).containsExactly("bench", "curl", "row")

        viewModel.onEvent(ExerciseLibraryEvent.EquipmentToggled(Equipment.DUMBBELL))
        assertThat(viewModel.uiState.value.exercises.map { it.id }).containsExactly("curl", "row")

        viewModel.onEvent(ExerciseLibraryEvent.MuscleToggled(Muscle.BICEPS))
        assertThat(viewModel.uiState.value.exercises.map { it.id }).containsExactly("curl")
        assertThat(viewModel.uiState.value.filter.activeFilterCount).isEqualTo(2)

        viewModel.onEvent(ExerciseLibraryEvent.FiltersCleared)
        viewModel.onEvent(ExerciseLibraryEvent.QueryChanged("row"))
        assertThat(viewModel.uiState.value.exercises.map { it.id }).containsExactly("row")
        assertThat(viewModel.uiState.value.totalCount).isEqualTo(3)
    }

    @Test
    fun `toggling a filter twice removes it`() = runTest {
        val viewModel = ExerciseLibraryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onEvent(ExerciseLibraryEvent.MuscleToggled(Muscle.LATS))
        viewModel.onEvent(ExerciseLibraryEvent.MuscleToggled(Muscle.LATS))

        assertThat(viewModel.uiState.value.filter.muscles).isEmpty()
        assertThat(viewModel.uiState.value.exercises).hasSize(3)
    }
}
