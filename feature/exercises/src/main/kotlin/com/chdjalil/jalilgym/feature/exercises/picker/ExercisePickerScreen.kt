package com.chdjalil.jalilgym.feature.exercises.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.ui.ExerciseImage
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.exercises.R
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryEvent
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryUiState
import com.chdjalil.jalilgym.feature.exercises.library.ExerciseLibraryViewModel
import com.chdjalil.jalilgym.feature.exercises.library.SearchField

/** Multi-select exercise list; [onDone] receives the ids in the order they were picked. */
@Composable
internal fun ExercisePickerScreen(
    onClose: () -> Unit,
    onDone: (List<String>) -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerContent(uiState, viewModel::onEvent, onClose, onDone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExercisePickerContent(
    uiState: ExerciseLibraryUiState,
    onEvent: (ExerciseLibraryEvent) -> Unit,
    onClose: () -> Unit,
    onDone: (List<String>) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_exercises_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(JgIcons.Close), stringResource(R.string.feature_exercises_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            JgPrimaryButton(
                onClick = { onDone(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("picker_done"),
            ) {
                Text(
                    if (selected.isEmpty()) {
                        stringResource(R.string.feature_exercises_picker_none)
                    } else {
                        pluralStringResource(R.plurals.feature_exercises_picker_add, selected.size, selected.size)
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SearchField(uiState.filter.query) { onEvent(ExerciseLibraryEvent.QueryChanged(it)) }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Muscle.entries) { muscle ->
                    FilterChip(
                        selected = muscle in uiState.filter.muscles,
                        onClick = { onEvent(ExerciseLibraryEvent.MuscleToggled(muscle)) },
                        label = { Text(stringResource(muscle.labelRes)) },
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    val checked = exercise.id in selected
                    PickerRow(exercise, checked) {
                        selected = if (checked) selected - exercise.id else selected + exercise.id
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(exercise: Exercise, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        headlineContent = { Text(exercise.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.feature_exercises_row_subtitle,
                    exercise.primaryMuscles.map { stringResource(it.labelRes) }.joinToString(", "),
                    stringResource(exercise.equipment.labelRes),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            ExerciseImage(
                path = exercise.images.firstOrNull(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        },
        // The whole row is the toggle; the checkbox is visual only.
        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}
