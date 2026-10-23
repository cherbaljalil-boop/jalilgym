package com.chdjalil.jalilgym.feature.programs.day

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.program.isLinkedToNext
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramExercise
import com.chdjalil.jalilgym.feature.programs.R
import com.chdjalil.jalilgym.feature.programs.ui.BackTopBar
import com.chdjalil.jalilgym.feature.programs.ui.ExerciseSlotRow
import com.chdjalil.jalilgym.feature.programs.ui.TextInputDialog

@Composable
internal fun DayEditorScreen(
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
    viewModel: DayEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DayEditorContent(uiState, viewModel::onEvent, onBack, onAddExercises)
}

@Composable
internal fun DayEditorContent(
    uiState: DayEditorUiState,
    onEvent: (DayEditorEvent) -> Unit,
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
) {
    val day = (uiState as? DayEditorUiState.Ready)?.day
    var renaming by rememberSaveable { mutableStateOf(false) }
    var editingSlotId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            BackTopBar(title = day?.name.orEmpty(), onBack = onBack) {
                if (day != null) {
                    IconButton(onClick = { renaming = true }) {
                        Icon(painterResource(JgIcons.Edit), stringResource(R.string.feature_programs_rename_day))
                    }
                }
            }
        },
        floatingActionButton = {
            if (day != null) {
                ExtendedFloatingActionButton(
                    onClick = onAddExercises,
                    icon = { Icon(painterResource(JgIcons.Add), contentDescription = null) },
                    text = { Text(stringResource(R.string.feature_programs_add_exercises)) },
                    modifier = Modifier.testTag("add_exercises"),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (uiState) {
                DayEditorUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DayEditorUiState.NotFound -> Text(stringResource(R.string.feature_programs_day_missing), Modifier.align(Alignment.Center))
                is DayEditorUiState.Ready ->
                    if (uiState.day.exercises.isEmpty()) {
                        EmptyDay(Modifier.align(Alignment.Center))
                    } else {
                        ExerciseList(uiState.day, onEvent, onEdit = { editingSlotId = it })
                    }
            }
        }
    }

    if (renaming && day != null) {
        TextInputDialog(
            title = stringResource(R.string.feature_programs_rename_day),
            initial = day.name,
            confirmLabel = stringResource(R.string.feature_programs_save),
            onConfirm = {
                onEvent(DayEditorEvent.Rename(it))
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
    day?.exercises?.find { it.id == editingSlotId }?.let { slot ->
        PrescriptionSheet(
            slot = slot,
            onSave = {
                onEvent(DayEditorEvent.UpdatePrescription(slot.id, it))
                editingSlotId = null
            },
            onDismiss = { editingSlotId = null },
        )
    }
}

@Composable
private fun EmptyDay(modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_programs_day_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feature_programs_day_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseList(day: ProgramDay, onEvent: (DayEditorEvent) -> Unit, onEdit: (String) -> Unit) {
    val groups = day.exercises.map { it.supersetGroup }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        itemsIndexed(day.exercises, key = { _, slot -> slot.id }) { index, slot ->
            val inSuperset = groups.isLinkedToNext(index) || groups.isLinkedToNext(index - 1)
            ExerciseSlotRow(
                slot = slot,
                inSuperset = inSuperset,
                modifier = Modifier.clickable { onEdit(slot.id) },
                trailing = {
                    SlotMenu(
                        slot = slot,
                        canMoveUp = index > 0,
                        canMoveDown = index < day.exercises.lastIndex,
                        onEvent = onEvent,
                        onEdit = { onEdit(slot.id) },
                    )
                },
            )
            if (index < day.exercises.lastIndex) {
                SupersetToggle(linked = groups.isLinkedToNext(index)) { onEvent(DayEditorEvent.ToggleSupersetWithNext(index)) }
            }
        }
    }
}

/** Sits between two exercises; links them into a superset (or unlinks them). */
@Composable
private fun SupersetToggle(linked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 72.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onToggle) {
            Icon(painterResource(if (linked) JgIcons.LinkOff else JgIcons.Link), contentDescription = null)
            Text(
                stringResource(if (linked) R.string.feature_programs_unlink_superset else R.string.feature_programs_link_superset),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SlotMenu(
    slot: ProgramExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEvent: (DayEditorEvent) -> Unit,
    onEdit: () -> Unit,
) {
    var open by rememberSaveable(slot.id) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(JgIcons.More), stringResource(R.string.feature_programs_exercise_options, slot.exercise.name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_programs_edit_targets)) },
                onClick = {
                    open = false
                    onEdit()
                },
            )
            if (canMoveUp) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_programs_move_up)) },
                    leadingIcon = { Icon(painterResource(JgIcons.ArrowUp), null) },
                    onClick = {
                        open = false
                        onEvent(DayEditorEvent.Move(slot.id, -1))
                    },
                )
            }
            if (canMoveDown) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_programs_move_down)) },
                    leadingIcon = { Icon(painterResource(JgIcons.ArrowDown), null) },
                    onClick = {
                        open = false
                        onEvent(DayEditorEvent.Move(slot.id, 1))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_programs_remove)) },
                leadingIcon = { Icon(painterResource(JgIcons.Delete), null) },
                onClick = {
                    open = false
                    onEvent(DayEditorEvent.Remove(slot.id))
                },
            )
        }
    }
}
