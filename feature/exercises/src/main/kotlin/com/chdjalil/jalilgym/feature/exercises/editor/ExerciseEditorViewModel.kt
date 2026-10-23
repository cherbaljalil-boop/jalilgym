package com.chdjalil.jalilgym.feature.exercises.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseDraftError
import com.chdjalil.jalilgym.core.domain.exercise.validate
import com.chdjalil.jalilgym.core.domain.repository.ExerciseImageStore
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.ExerciseDraft
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.feature.exercises.navigation.EXERCISE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseEditorUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val draft: ExerciseDraft = ExerciseDraft(),
    /** One step per line; split into instructions on save. */
    val instructionsText: String = "",
    /** Errors are only shown after the first save attempt. */
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val isImportingPhoto: Boolean = false,
    val photoFailed: Boolean = false,
    val savedId: String? = null,
) {
    val errors: Set<ExerciseDraftError> get() = draft.validate()
    val visibleErrors: Set<ExerciseDraftError> get() = if (showErrors) errors else emptySet()
}

sealed interface ExerciseEditorEvent {
    data class NameChanged(val name: String) : ExerciseEditorEvent

    data class InstructionsChanged(val text: String) : ExerciseEditorEvent

    data class EquipmentSelected(val equipment: Equipment) : ExerciseEditorEvent

    data class MechanicsSelected(val mechanics: Mechanics) : ExerciseEditorEvent

    data class ForceSelected(val force: Force) : ExerciseEditorEvent

    data class DifficultySelected(val difficulty: Difficulty) : ExerciseEditorEvent

    data class TrackingSelected(val trackingType: TrackingType) : ExerciseEditorEvent

    data class PrimaryMuscleToggled(val muscle: Muscle) : ExerciseEditorEvent

    data class SecondaryMuscleToggled(val muscle: Muscle) : ExerciseEditorEvent

    data class PhotoPicked(val uri: String) : ExerciseEditorEvent

    data object PhotoRemoved : ExerciseEditorEvent

    data object SaveClicked : ExerciseEditorEvent
}

@HiltViewModel
class ExerciseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val imageStore: ExerciseImageStore,
) : ViewModel() {

    private val exerciseId: String? = savedStateHandle[EXERCISE_ID_ARG]

    private val _uiState = MutableStateFlow(
        ExerciseEditorUiState(isEditing = exerciseId != null, isLoading = exerciseId != null),
    )
    val uiState: StateFlow<ExerciseEditorUiState> = _uiState.asStateFlow()

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                val exercise = exerciseRepository.observeExercise(exerciseId).first()
                _uiState.update { state ->
                    if (exercise == null || exercise.isBuiltIn) {
                        state.copy(isLoading = false)
                    } else {
                        state.copy(
                            isLoading = false,
                            draft = ExerciseDraft(
                                id = exercise.id,
                                name = exercise.name,
                                instructions = exercise.instructions,
                                equipment = exercise.equipment,
                                mechanics = exercise.mechanics,
                                force = exercise.force,
                                difficulty = exercise.difficulty,
                                trackingType = exercise.trackingType,
                                primaryMuscles = exercise.primaryMuscles.toSet(),
                                secondaryMuscles = exercise.secondaryMuscles.toSet(),
                                imagePath = exercise.images.firstOrNull(),
                            ),
                            instructionsText = exercise.instructions.joinToString("\n"),
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ExerciseEditorEvent) {
        when (event) {
            is ExerciseEditorEvent.NameChanged -> updateDraft { it.copy(name = event.name) }
            is ExerciseEditorEvent.InstructionsChanged -> _uiState.update { it.copy(instructionsText = event.text) }
            is ExerciseEditorEvent.EquipmentSelected -> updateDraft { it.copy(equipment = event.equipment) }
            is ExerciseEditorEvent.MechanicsSelected -> updateDraft { it.copy(mechanics = event.mechanics) }
            is ExerciseEditorEvent.ForceSelected -> updateDraft { it.copy(force = event.force) }
            is ExerciseEditorEvent.DifficultySelected -> updateDraft { it.copy(difficulty = event.difficulty) }
            is ExerciseEditorEvent.TrackingSelected -> updateDraft { it.copy(trackingType = event.trackingType) }
            is ExerciseEditorEvent.PrimaryMuscleToggled -> updateDraft { draft ->
                val primary = draft.primaryMuscles.toggle(event.muscle)
                // A muscle can't be both; promoting it to primary removes it from secondary.
                draft.copy(primaryMuscles = primary, secondaryMuscles = draft.secondaryMuscles - primary)
            }
            is ExerciseEditorEvent.SecondaryMuscleToggled -> updateDraft { draft ->
                if (event.muscle in draft.primaryMuscles) {
                    draft
                } else {
                    draft.copy(secondaryMuscles = draft.secondaryMuscles.toggle(event.muscle))
                }
            }
            is ExerciseEditorEvent.PhotoPicked -> importPhoto(event.uri)
            ExerciseEditorEvent.PhotoRemoved -> updateDraft { it.copy(imagePath = null) }
            ExerciseEditorEvent.SaveClicked -> save()
        }
    }

    private fun updateDraft(transform: (ExerciseDraft) -> ExerciseDraft) {
        _uiState.update { it.copy(draft = transform(it.draft)) }
    }

    private fun importPhoto(uri: String) {
        _uiState.update { it.copy(isImportingPhoto = true, photoFailed = false) }
        viewModelScope.launch {
            val path = runCatching { imageStore.import(uri) }.getOrNull()
            _uiState.update { state ->
                state.copy(
                    isImportingPhoto = false,
                    photoFailed = path == null,
                    draft = if (path != null) state.draft.copy(imagePath = path) else state.draft,
                )
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val draft = state.draft.copy(
            name = state.draft.name.trim(),
            instructions = state.instructionsText.lines().map(String::trim).filter(String::isNotEmpty),
        )
        if (draft.validate().isNotEmpty()) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true, showErrors = true) }
        viewModelScope.launch {
            val id = exerciseRepository.saveCustomExercise(draft)
            _uiState.update { it.copy(isSaving = false, savedId = id) }
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
