package com.chdjalil.jalilgym.feature.programs.builder

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.component.JgSecondaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.feature.programs.R
import com.chdjalil.jalilgym.feature.programs.ui.BackTopBar
import com.chdjalil.jalilgym.feature.programs.ui.SectionTitle
import com.chdjalil.jalilgym.feature.programs.ui.Stepper
import com.chdjalil.jalilgym.feature.programs.ui.TextInputDialog

@Composable
internal fun ProgramBuilderScreen(
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    viewModel: ProgramBuilderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val duplicated by viewModel.duplicatedProgramId.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onBack() }
    LaunchedEffect(duplicated) { duplicated?.let(onOpenProgram) }
    ProgramBuilderContent(uiState, viewModel::onEvent, onBack, onOpenDay)
}

/** Which dialog is open; days are addressed by id. */
private sealed interface BuilderDialog {
    data object RenameProgram : BuilderDialog

    data object AddDay : BuilderDialog

    data class RenameDay(val day: ProgramDay) : BuilderDialog

    data class DeleteDay(val day: ProgramDay) : BuilderDialog

    data object DeleteProgram : BuilderDialog
}

@Composable
internal fun ProgramBuilderContent(
    uiState: ProgramBuilderUiState,
    onEvent: (ProgramBuilderEvent) -> Unit,
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
) {
    var dialog by remember { mutableStateOf<BuilderDialog?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val ready = uiState as? ProgramBuilderUiState.Ready
    val copyName = ready?.let { stringResource(R.string.feature_programs_copy_name, it.program.name) }

    Scaffold(
        topBar = {
            BackTopBar(title = ready?.program?.name.orEmpty(), onBack = onBack) {
                if (ready != null) {
                    IconButton(onClick = { dialog = BuilderDialog.RenameProgram }) {
                        Icon(painterResource(JgIcons.Edit), stringResource(R.string.feature_programs_rename))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(JgIcons.More), stringResource(R.string.feature_programs_more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feature_programs_duplicate_program)) },
                                onClick = {
                                    menuOpen = false
                                    copyName?.let { onEvent(ProgramBuilderEvent.Duplicate(it)) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feature_programs_delete_program)) },
                                onClick = {
                                    menuOpen = false
                                    dialog = BuilderDialog.DeleteProgram
                                },
                            )
                        }
                    }
                }
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
                ProgramBuilderUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ProgramBuilderUiState.NotFound -> Text(
                    stringResource(R.string.feature_programs_program_missing),
                    Modifier.align(Alignment.Center),
                )
                is ProgramBuilderUiState.Ready -> BuilderBody(
                    program = uiState.program,
                    isActive = uiState.isActive,
                    onEvent = onEvent,
                    onOpenDay = onOpenDay,
                    onAddDay = { dialog = BuilderDialog.AddDay },
                    onRenameDay = { dialog = BuilderDialog.RenameDay(it) },
                    onDeleteDay = { dialog = BuilderDialog.DeleteDay(it) },
                )
            }
        }
    }

    val program = ready?.program
    when (val current = dialog) {
        null -> Unit
        BuilderDialog.RenameProgram -> TextInputDialog(
            title = stringResource(R.string.feature_programs_rename_program),
            initial = program?.name.orEmpty(),
            confirmLabel = stringResource(R.string.feature_programs_save),
            onConfirm = {
                onEvent(ProgramBuilderEvent.Rename(it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        BuilderDialog.AddDay -> TextInputDialog(
            title = stringResource(R.string.feature_programs_add_day),
            initial = stringResource(R.string.feature_programs_default_day_name, (program?.days?.size ?: 0) + 1),
            confirmLabel = stringResource(R.string.feature_programs_add),
            onConfirm = {
                onEvent(ProgramBuilderEvent.AddDay(it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is BuilderDialog.RenameDay -> TextInputDialog(
            title = stringResource(R.string.feature_programs_rename_day),
            initial = current.day.name,
            confirmLabel = stringResource(R.string.feature_programs_save),
            onConfirm = {
                onEvent(ProgramBuilderEvent.RenameDay(current.day.id, it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is BuilderDialog.DeleteDay -> ConfirmDialog(
            title = stringResource(R.string.feature_programs_delete_day_title, current.day.name),
            body = stringResource(R.string.feature_programs_delete_day_body),
            onConfirm = {
                onEvent(ProgramBuilderEvent.DeleteDay(current.day.id))
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        BuilderDialog.DeleteProgram -> ConfirmDialog(
            title = stringResource(R.string.feature_programs_delete_program_title, program?.name.orEmpty()),
            body = stringResource(R.string.feature_programs_delete_program_body),
            onConfirm = {
                onEvent(ProgramBuilderEvent.Delete)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun BuilderBody(
    program: Program,
    isActive: Boolean,
    onEvent: (ProgramBuilderEvent) -> Unit,
    onOpenDay: (String) -> Unit,
    onAddDay: () -> Unit,
    onRenameDay: (ProgramDay) -> Unit,
    onDeleteDay: (ProgramDay) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (isActive) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.feature_programs_active_program)) })
            } else {
                JgPrimaryButton(onClick = { onEvent(ProgramBuilderEvent.Activate) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_programs_make_active))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_programs_days_per_week_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.feature_programs_days_per_week_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Stepper(
                    value = program.daysPerWeek,
                    onValueChange = { onEvent(ProgramBuilderEvent.SetDaysPerWeek(it)) },
                    range = 1..7,
                    label = stringResource(R.string.feature_programs_days_per_week_label),
                )
            }
        }
        item { SectionTitle(stringResource(R.string.feature_programs_workouts_in_rotation)) }
        itemsIndexed(program.days, key = { _, day -> day.id }) { index, day ->
            DayCard(
                day = day,
                isFirst = index == 0,
                isLast = index == program.days.lastIndex,
                onOpen = { onOpenDay(day.id) },
                onRename = { onRenameDay(day) },
                onDuplicate = { onEvent(ProgramBuilderEvent.DuplicateDay(day.id)) },
                onMove = { onEvent(ProgramBuilderEvent.MoveDay(day.id, it)) },
                onDelete = { onDeleteDay(day) },
            )
        }
        item {
            JgSecondaryButton(onClick = onAddDay, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(JgIcons.Add), contentDescription = null)
                Text(stringResource(R.string.feature_programs_add_day), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun DayCard(
    day: ProgramDay,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by rememberSaveable(day.id) { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.feature_programs_day_number, day.dayIndex + 1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(day.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (day.exercises.isEmpty()) {
                        stringResource(R.string.feature_programs_day_empty)
                    } else {
                        pluralStringResource(R.plurals.feature_programs_exercise_count, day.exercises.size, day.exercises.size) +
                            " · " + day.exercises.joinToString(", ") { it.exercise.name }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(JgIcons.More), stringResource(R.string.feature_programs_day_options, day.name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem(R.string.feature_programs_rename) {
                        menuOpen = false
                        onRename()
                    }
                    MenuItem(R.string.feature_programs_duplicate) {
                        menuOpen = false
                        onDuplicate()
                    }
                    if (!isFirst) {
                        MenuItem(R.string.feature_programs_move_up) {
                            menuOpen = false
                            onMove(-1)
                        }
                    }
                    if (!isLast) {
                        MenuItem(R.string.feature_programs_move_down) {
                            menuOpen = false
                            onMove(1)
                        }
                    }
                    MenuItem(R.string.feature_programs_delete) {
                        menuOpen = false
                        onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}

@Composable
internal fun ConfirmDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.feature_programs_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_programs_cancel)) } },
    )
}
