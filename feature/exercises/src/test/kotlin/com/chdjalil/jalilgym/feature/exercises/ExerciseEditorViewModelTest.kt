package com.chdjalil.jalilgym.feature.exercises

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseDraftError
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.ExerciseDraft
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseImageStore
import com.chdjalil.jalilgym.core.testing.repository.FakeExerciseRepository
import com.chdjalil.jalilgym.feature.exercises.editor.ExerciseEditorEvent
import com.chdjalil.jalilgym.feature.exercises.editor.ExerciseEditorViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExerciseEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository()
    private val imageStore = FakeExerciseImageStore()

    private fun viewModel(id: String? = null) = ExerciseEditorViewModel(
        SavedStateHandle(mapOf("exerciseId" to id)),
        repository,
        imageStore,
    )

    @Test
    fun `errors appear only after trying to save`() = runTest {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value.visibleErrors).isEmpty()

        viewModel.onEvent(ExerciseEditorEvent.SaveClicked)

        assertThat(viewModel.uiState.value.visibleErrors)
            .containsExactly(ExerciseDraftError.NAME_BLANK, ExerciseDraftError.NO_PRIMARY_MUSCLE)
        assertThat(repository.savedDrafts).isEmpty()
    }

    @Test
    fun `saves a trimmed draft with instructions split per line`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ExerciseEditorEvent.NameChanged("  Landmine Press "))
        viewModel.onEvent(ExerciseEditorEvent.PrimaryMuscleToggled(Muscle.FRONT_DELTS))
        viewModel.onEvent(ExerciseEditorEvent.SecondaryMuscleToggled(Muscle.TRICEPS))
        viewModel.onEvent(ExerciseEditorEvent.EquipmentSelected(Equipment.OTHER))
        viewModel.onEvent(ExerciseEditorEvent.InstructionsChanged("Hold the bar end\n\n  Press up and forward  "))
        viewModel.onEvent(ExerciseEditorEvent.SaveClicked)

        val saved = repository.savedDrafts.single()
        assertThat(saved.name).isEqualTo("Landmine Press")
        assertThat(saved.instructions).containsExactly("Hold the bar end", "Press up and forward").inOrder()
        assertThat(saved.equipment).isEqualTo(Equipment.OTHER)
        assertThat(saved.secondaryMuscles).containsExactly(Muscle.TRICEPS)
        assertThat(viewModel.uiState.value.savedId).isEqualTo("custom-1")
    }

    @Test
    fun `making a muscle primary removes it from secondary`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ExerciseEditorEvent.SecondaryMuscleToggled(Muscle.CHEST))
        viewModel.onEvent(ExerciseEditorEvent.PrimaryMuscleToggled(Muscle.CHEST))
        viewModel.onEvent(ExerciseEditorEvent.SecondaryMuscleToggled(Muscle.CHEST))

        assertThat(viewModel.uiState.value.draft.primaryMuscles).containsExactly(Muscle.CHEST)
        assertThat(viewModel.uiState.value.draft.secondaryMuscles).isEmpty()
    }

    @Test
    fun `editing loads the custom exercise`() = runTest {
        val id = repository.saveCustomExercise(
            ExerciseDraft(name = "Sled Push", primaryMuscles = setOf(Muscle.QUADS), instructions = listOf("Push")),
        )

        val viewModel = viewModel(id)

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.draft.name).isEqualTo("Sled Push")
        assertThat(state.instructionsText).isEqualTo("Push")
    }

    @Test
    fun `built-in exercises are not loaded for editing`() = runTest {
        repository.emit(listOf(sampleExercise("bench")))

        val viewModel = viewModel("bench")

        assertThat(viewModel.uiState.value.draft.name).isEmpty()
    }

    @Test
    fun `picked photo is imported, failures are reported`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ExerciseEditorEvent.PhotoPicked("content://media/42"))
        assertThat(viewModel.uiState.value.draft.imagePath).isEqualTo("/data/exercise_images/42.jpg")

        imageStore.failImports = true
        viewModel.onEvent(ExerciseEditorEvent.PhotoPicked("content://media/43"))
        assertThat(viewModel.uiState.value.photoFailed).isTrue()
        assertThat(viewModel.uiState.value.draft.imagePath).isEqualTo("/data/exercise_images/42.jpg")
    }
}
