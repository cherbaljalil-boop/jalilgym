package com.chdjalil.jalilgym.feature.exercises.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.exercise.EXERCISE_NAME_MAX_LENGTH
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseDraftError
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.ui.ExerciseImage
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.exercises.R
import com.chdjalil.jalilgym.feature.exercises.library.ChipGroup

@Composable
internal fun ExerciseEditorScreen(
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: ExerciseEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.savedId) { uiState.savedId?.let(onSaved) }

    // System photo picker: no storage permission needed.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onEvent(ExerciseEditorEvent.PhotoPicked(it.toString())) }
    }
    ExerciseEditorContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onPickPhoto = {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseEditorContent(
    uiState: ExerciseEditorUiState,
    onEvent: (ExerciseEditorEvent) -> Unit,
    onClose: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.feature_exercises_edit_title else R.string.feature_exercises_new_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(JgIcons.Close), stringResource(R.string.feature_exercises_cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onEvent(ExerciseEditorEvent.SaveClicked) },
                        enabled = !uiState.isSaving && !uiState.isLoading,
                        modifier = Modifier.testTag("save_exercise"),
                    ) { Text(stringResource(R.string.feature_exercises_save)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            EditorForm(uiState, onEvent, onPickPhoto, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorForm(
    uiState: ExerciseEditorUiState,
    onEvent: (ExerciseEditorEvent) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier,
) {
    val draft = uiState.draft
    val errors = uiState.visibleErrors
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PhotoPicker(uiState, onPickPhoto, onRemove = { onEvent(ExerciseEditorEvent.PhotoRemoved) })

        OutlinedTextField(
            value = draft.name,
            onValueChange = { onEvent(ExerciseEditorEvent.NameChanged(it)) },
            label = { Text(stringResource(R.string.feature_exercises_name)) },
            singleLine = true,
            isError = ExerciseDraftError.NAME_BLANK in errors || ExerciseDraftError.NAME_TOO_LONG in errors,
            supportingText = {
                when {
                    ExerciseDraftError.NAME_BLANK in errors ->
                        Text(stringResource(R.string.feature_exercises_error_name_blank))
                    ExerciseDraftError.NAME_TOO_LONG in errors ->
                        Text(stringResource(R.string.feature_exercises_error_name_long, EXERCISE_NAME_MAX_LENGTH))
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exercise_name"),
        )

        Section(R.string.feature_exercises_primary_muscles)
        ChipGroup(Muscle.entries, draft.primaryMuscles, { it.labelRes }) {
            onEvent(ExerciseEditorEvent.PrimaryMuscleToggled(it))
        }
        if (ExerciseDraftError.NO_PRIMARY_MUSCLE in errors) {
            Text(
                stringResource(R.string.feature_exercises_error_no_primary),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Section(R.string.feature_exercises_secondary_muscles)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Muscle.entries.forEach { muscle ->
                FilterChip(
                    selected = muscle in draft.secondaryMuscles,
                    onClick = { onEvent(ExerciseEditorEvent.SecondaryMuscleToggled(muscle)) },
                    enabled = muscle !in draft.primaryMuscles,
                    label = { Text(stringResource(muscle.labelRes)) },
                )
            }
        }

        Section(R.string.feature_exercises_filter_equipment)
        SingleChoice(Equipment.entries, draft.equipment, { it.labelRes }) {
            onEvent(ExerciseEditorEvent.EquipmentSelected(it))
        }
        Section(R.string.feature_exercises_filter_type)
        SingleChoice(Mechanics.entries, draft.mechanics, { it.labelRes }) {
            onEvent(ExerciseEditorEvent.MechanicsSelected(it))
        }
        Section(R.string.feature_exercises_force)
        SingleChoice(Force.entries, draft.force, { it.labelRes }) { onEvent(ExerciseEditorEvent.ForceSelected(it)) }
        Section(R.string.feature_exercises_filter_level)
        SingleChoice(Difficulty.entries, draft.difficulty, { it.labelRes }) {
            onEvent(ExerciseEditorEvent.DifficultySelected(it))
        }
        Section(R.string.feature_exercises_tracking)
        SingleChoice(TrackingType.entries, draft.trackingType, { it.labelRes }) {
            onEvent(ExerciseEditorEvent.TrackingSelected(it))
        }

        OutlinedTextField(
            value = uiState.instructionsText,
            onValueChange = { onEvent(ExerciseEditorEvent.InstructionsChanged(it)) },
            label = { Text(stringResource(R.string.feature_exercises_instructions)) },
            supportingText = { Text(stringResource(R.string.feature_exercises_instructions_hint)) },
            minLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhotoPicker(uiState: ExerciseEditorUiState, onPick: () -> Unit, onRemove: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            ExerciseImage(path = uiState.draft.imagePath, contentDescription = null, modifier = Modifier.fillMaxSize())
            if (uiState.isImportingPhoto) CircularProgressIndicator(Modifier.size(32.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = onPick, enabled = !uiState.isImportingPhoto) {
                Icon(painterResource(JgIcons.AddPhoto), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(
                        if (uiState.draft.imagePath == null) R.string.feature_exercises_add_photo else R.string.feature_exercises_change_photo,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (uiState.draft.imagePath != null) {
                TextButton(onClick = onRemove) { Text(stringResource(R.string.feature_exercises_remove_photo)) }
            }
            if (uiState.photoFailed) {
                Text(
                    stringResource(R.string.feature_exercises_photo_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SingleChoice(options: List<T>, selected: T, label: (T) -> Int, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(stringResource(label(option))) },
                leadingIcon = if (option == selected) {
                    { Icon(painterResource(JgIcons.Check), null, Modifier.size(18.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun Section(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview
@Composable
private fun EditorPreview() {
    JalilgymTheme {
        ExerciseEditorContent(ExerciseEditorUiState(), onEvent = {}, onClose = {}, onPickPhoto = {})
    }
}
