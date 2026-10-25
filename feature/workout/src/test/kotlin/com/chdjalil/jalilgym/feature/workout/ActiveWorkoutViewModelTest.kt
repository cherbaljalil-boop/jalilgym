package com.chdjalil.jalilgym.feature.workout

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.domain.workout.CompleteSetUseCase
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeRestTimer
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeWorkoutRepository
import com.chdjalil.jalilgym.core.ui.ExercisePickerResult
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutEvent
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutUiState
import com.chdjalil.jalilgym.feature.workout.active.ActiveWorkoutViewModel
import com.chdjalil.jalilgym.feature.workout.active.next
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ActiveWorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = sampleExercise("bench", "Bench")
    private val curl = sampleExercise("curl", "Curl").copy(mechanics = Mechanics.ISOLATION)
    private val workouts = FakeWorkoutRepository(exercises = listOf(bench, curl))
    private val timer = FakeRestTimer()
    private val preferences = FakeUserPreferencesRepository()
    private val handle = SavedStateHandle()

    private fun TestScope.viewModel(): ActiveWorkoutViewModel {
        val vm = ActiveWorkoutViewModel(handle, workouts, timer, CompleteSetUseCase(workouts, preferences, timer), preferences)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private val ActiveWorkoutViewModel.active get() = uiState.value as ActiveWorkoutUiState.Active

    @Test
    fun `no workout means nothing to show`() = runTest {
        assertThat(viewModel().uiState.value).isEqualTo(ActiveWorkoutUiState.None)
    }

    @Test
    fun `logging sets edits values and starts rest`() = runTest {
        val id = workouts.startEmpty("Quick")
        val vm = viewModel()

        handle[ExercisePickerResult.KEY] = arrayListOf("bench", "curl")
        val set = vm.active.session.exercises.first().sets.first()

        vm.onEvent(ActiveWorkoutEvent.EditSet(set.id) { it.copy(weightKg = 60.0) })
        vm.onEvent(ActiveWorkoutEvent.EditSet(set.id) { it.copy(reps = 8) })
        vm.onEvent(ActiveWorkoutEvent.ToggleComplete(vm.active.session.exercises.first().sets.first()))

        val logged = vm.active.session.exercises.first().sets.first()
        assertThat(logged.weightKg).isEqualTo(60.0)
        assertThat(logged.reps).isEqualTo(8)
        assertThat(logged.completed).isTrue()
        assertThat(vm.active.rest).isNotNull()

        vm.onEvent(ActiveWorkoutEvent.AdjustRest(15))
        assertThat(vm.active.rest!!.totalMillis).isEqualTo((180 + 15) * 1000L)
        vm.onEvent(ActiveWorkoutEvent.SkipRest)
        assertThat(vm.active.rest).isNull()

        // Unchecking doesn't start another timer.
        vm.onEvent(ActiveWorkoutEvent.ToggleComplete(logged))
        assertThat(vm.active.session.exercises.first().sets.first().completed).isFalse()
        assertThat(vm.active.rest).isNull()
        assertThat(vm.active.session.id).isEqualTo(id)
    }

    @Test
    fun `set types cycle and sets can be added or removed`() = runTest {
        workouts.startEmpty("Quick")
        val vm = viewModel()
        handle[ExercisePickerResult.KEY] = arrayListOf("bench")
        val exercise = vm.active.session.exercises.single()

        vm.onEvent(ActiveWorkoutEvent.CycleType(exercise.sets.first()))
        vm.onEvent(ActiveWorkoutEvent.AddSet(exercise.id))
        vm.onEvent(ActiveWorkoutEvent.RemoveSet(exercise.sets[1].id))
        vm.onEvent(ActiveWorkoutEvent.AddWarmups(exercise.id, listOf(20.0 to 10)))

        val sets = vm.active.session.exercises.single().sets
        assertThat(sets.map { it.type }).containsExactly(SetType.WARMUP, SetType.WARMUP, SetType.WORKING, SetType.WORKING).inOrder()
        assertThat(SetType.FAILURE.next()).isEqualTo(SetType.WORKING)
    }

    @Test
    fun `picker swaps the requested exercise`() = runTest {
        workouts.startEmpty("Quick")
        val vm = viewModel()
        handle[ExercisePickerResult.KEY] = arrayListOf("bench")
        val exercise = vm.active.session.exercises.single()

        vm.onEvent(ActiveWorkoutEvent.RequestSwap(exercise.id))
        handle[ExercisePickerResult.KEY] = arrayListOf("curl")

        assertThat(vm.active.session.exercises.map { it.exercise.id }).containsExactly("curl")

        vm.onEvent(ActiveWorkoutEvent.RequestAdd)
        handle[ExercisePickerResult.KEY] = arrayListOf("bench")
        assertThat(vm.active.session.exercises.map { it.exercise.id }).containsExactly("curl", "bench").inOrder()
    }

    @Test
    fun `finishing and discarding end the workout`() = runTest {
        workouts.startEmpty("Quick")
        val vm = viewModel()
        timer.start(60, "x")

        vm.onEvent(ActiveWorkoutEvent.Finish("good"))

        assertThat(vm.finishedSessionId.value).isNotNull()
        assertThat(vm.uiState.value).isEqualTo(ActiveWorkoutUiState.None)
        assertThat(timer.state.value).isNull()

        workouts.startEmpty("Again")
        val second = viewModel()
        second.onEvent(ActiveWorkoutEvent.Discard)
        assertThat(second.discarded.value).isTrue()
    }
}
