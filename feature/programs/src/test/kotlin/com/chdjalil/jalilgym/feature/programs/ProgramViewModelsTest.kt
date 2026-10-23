package com.chdjalil.jalilgym.feature.programs

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeWorkoutRepository
import com.chdjalil.jalilgym.core.ui.ExercisePickerResult
import com.chdjalil.jalilgym.feature.programs.builder.ProgramBuilderEvent
import com.chdjalil.jalilgym.feature.programs.builder.ProgramBuilderUiState
import com.chdjalil.jalilgym.feature.programs.builder.ProgramBuilderViewModel
import com.chdjalil.jalilgym.feature.programs.day.DayEditorEvent
import com.chdjalil.jalilgym.feature.programs.day.DayEditorUiState
import com.chdjalil.jalilgym.feature.programs.day.DayEditorViewModel
import com.chdjalil.jalilgym.feature.programs.preview.DayPreviewUiState
import com.chdjalil.jalilgym.feature.programs.preview.DayPreviewViewModel
import com.chdjalil.jalilgym.feature.programs.templates.TemplateGalleryViewModel
import com.chdjalil.jalilgym.feature.programs.train.TrainViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProgramViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val programs = programRepository()
    private val workouts = FakeWorkoutRepository(exercises = listOf(squat, curl, row), programs = programs)

    /** Keeps a WhileSubscribed state flow hot for the duration of the test. */
    private fun <T> TestScope.collect(flow: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
        return flow
    }

    @Test
    fun `using a template copies it and activates it when nothing is active`() = runTest {
        val viewModel = TemplateGalleryViewModel(programs)
        assertThat(collect(viewModel.uiState).value.templates.map { it.id }).containsExactly("tpl-ppl-3")

        viewModel.useTemplate("tpl-ppl-3")

        val created = viewModel.createdProgramId.value!!
        assertThat(programs.activeProgramId).isEqualTo(created)

        // A second template use doesn't steal the active slot.
        val second = TemplateGalleryViewModel(programs)
        second.useTemplate("tpl-ppl-3")
        assertThat(programs.activeProgramId).isEqualTo(created)
    }

    @Test
    fun `train screen lists programs and creates blank ones`() = runTest {
        val viewModel = TrainViewModel(programs, workouts)
        val state = collect(viewModel.uiState)
        assertThat(state.value.active).isNull()

        viewModel.createBlankProgram("Upper / Lower")

        assertThat(viewModel.createdProgramId.value).isNotNull()
        assertThat(state.value.programs.map { it.name }).containsExactly("Upper / Lower")
        viewModel.onNavigatedToCreated()
        assertThat(viewModel.createdProgramId.value).isNull()

        viewModel.startEmptyWorkout("Workout")
        assertThat(viewModel.workoutStarted.value).isTrue()
        assertThat(workouts.current.single().name).isEqualTo("Workout")
    }

    @Test
    fun `builder edits the program and reports deletion`() = runTest {
        val id = programs.copyProgram("tpl-ppl-3")
        val viewModel = ProgramBuilderViewModel(SavedStateHandle(mapOf("programId" to id)), programs)
        val state = collect(viewModel.uiState)

        viewModel.onEvent(ProgramBuilderEvent.Rename("  My PPL "))
        viewModel.onEvent(ProgramBuilderEvent.AddDay("Arms"))
        viewModel.onEvent(ProgramBuilderEvent.SetDaysPerWeek(4))
        viewModel.onEvent(ProgramBuilderEvent.Activate)
        val ready = state.value as ProgramBuilderUiState.Ready
        assertThat(ready.program.name).isEqualTo("My PPL")
        assertThat(ready.program.days.map { it.name }).containsExactly("Push", "Pull", "Legs", "Arms").inOrder()
        assertThat(ready.program.daysPerWeek).isEqualTo(4)
        assertThat(ready.isActive).isTrue()

        viewModel.onEvent(ProgramBuilderEvent.MoveDay(ready.program.days.last().id, -3))
        assertThat((state.value as ProgramBuilderUiState.Ready).program.days.first().name).isEqualTo("Arms")

        viewModel.onEvent(ProgramBuilderEvent.Rename("   "))
        assertThat((state.value as ProgramBuilderUiState.Ready).program.name).isEqualTo("My PPL")

        viewModel.onEvent(ProgramBuilderEvent.Delete)
        assertThat(viewModel.deleted.value).isTrue()
        assertThat(state.value).isEqualTo(ProgramBuilderUiState.NotFound)
        assertThat(programs.activeProgramId).isNull()
    }

    @Test
    fun `day editor adds picked exercises once and edits them`() = runTest {
        val id = programs.createProgram("Custom")
        val dayId = programs.currentPrograms.single { it.id == id }.days.single().id
        val handle = SavedStateHandle(mapOf("dayId" to dayId))
        val viewModel = DayEditorViewModel(handle, programs)
        val state = collect(viewModel.uiState)

        handle[ExercisePickerResult.KEY] = arrayListOf("squat", "curl", "row")

        var day = (state.value as DayEditorUiState.Ready).day
        assertThat(day.exercises.map { it.exercise.id }).containsExactly("squat", "curl", "row").inOrder()
        assertThat(handle.get<List<String>>(ExercisePickerResult.KEY)).isNull()

        viewModel.onEvent(DayEditorEvent.ToggleSupersetWithNext(0))
        viewModel.onEvent(DayEditorEvent.Move(day.exercises[2].id, -2))
        viewModel.onEvent(
            DayEditorEvent.UpdatePrescription(
                day.exercises[1].id,
                Prescription(targetSets = 4, repMin = 12, repMax = 15, restSeconds = 60, incrementKg = 1.0, progressionRule = ProgressionRule.LINEAR),
            ),
        )
        day = (state.value as DayEditorUiState.Ready).day
        assertThat(day.exercises.map { it.exercise.id }).containsExactly("row", "squat", "curl").inOrder()
        assertThat(day.exercises[1].supersetGroup).isEqualTo(day.exercises[2].supersetGroup)
        assertThat(day.exercises[2].targetSets).isEqualTo(4)
        assertThat(day.exercises[2].progressionRule).isEqualTo(ProgressionRule.LINEAR)

        viewModel.onEvent(DayEditorEvent.Remove(day.exercises[0].id))
        assertThat((state.value as DayEditorUiState.Ready).day.exercises).hasSize(2)
    }

    @Test
    fun `preview can make a day the next one`() = runTest {
        val id = programs.copyProgram("tpl-ppl-3")
        programs.activate(id)
        val legs = programs.currentPrograms.single { it.id == id }.days[2]
        val viewModel = DayPreviewViewModel(SavedStateHandle(mapOf("dayId" to legs.id)), programs, workouts)
        val state = collect(viewModel.uiState)

        val ready = state.value as DayPreviewUiState.Ready
        assertThat(ready.canMakeNext).isTrue()
        assertThat(ready.isNext).isFalse()

        viewModel.makeNext()

        assertThat(programs.activeNextDayIndex).isEqualTo(2)
        assertThat((state.value as DayPreviewUiState.Ready).isNext).isTrue()

        viewModel.startWorkout()
        assertThat(viewModel.startedSessionId.value).isNotNull()
        assertThat((state.value as DayPreviewUiState.Ready).workoutInProgress).isEqualTo("Legs")
    }
}
