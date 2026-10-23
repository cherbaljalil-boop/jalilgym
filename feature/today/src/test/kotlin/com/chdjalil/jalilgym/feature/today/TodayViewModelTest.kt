package com.chdjalil.jalilgym.feature.today

import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramType
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeProgramRepository
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
    private val template = Program(
        "tpl", "PPL", ProgramType.PPL, 3, true, null,
        listOf(
            ProgramDay("push", "tpl", 0, "Push", emptyList()),
            ProgramDay("pull", "tpl", 1, "Pull", emptyList()),
        ),
    )
    private val programs = FakeProgramRepository(templates = listOf(template))

    @Test
    fun `shows the next workout of the active program and can skip it`() = runTest {
        val viewModel = TodayViewModel(exercises, programs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.active).isNull()
        assertThat(viewModel.uiState.value.isLoading).isFalse()

        exercises.emit(List(5) { sampleExercise("e$it") })
        programs.activate(programs.copyProgram("tpl"))
        assertThat(viewModel.uiState.value.exerciseCount).isEqualTo(5)
        assertThat(viewModel.uiState.value.active!!.nextDay!!.name).isEqualTo("Push")

        viewModel.skipToNextDay()
        assertThat(viewModel.uiState.value.active!!.nextDay!!.name).isEqualTo("Pull")
    }
}
