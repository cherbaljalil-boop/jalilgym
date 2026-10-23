package com.chdjalil.jalilgym.feature.programs.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.component.JgSecondaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.ui.EntryCard
import com.chdjalil.jalilgym.feature.programs.R
import com.chdjalil.jalilgym.feature.programs.ui.SectionTitle
import com.chdjalil.jalilgym.feature.programs.ui.TextInputDialog

@Composable
internal fun TrainScreen(
    onOpenLibrary: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenProgram: (String) -> Unit,
    onOpenDay: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onWorkoutStarted: () -> Unit,
    viewModel: TrainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val created by viewModel.createdProgramId.collectAsStateWithLifecycle()
    val workoutStarted by viewModel.workoutStarted.collectAsStateWithLifecycle()
    val emptyName = stringResource(R.string.feature_programs_empty_workout_name)
    LaunchedEffect(workoutStarted) {
        if (workoutStarted) {
            viewModel.onWorkoutOpened()
            onWorkoutStarted()
        }
    }
    LaunchedEffect(created) {
        created?.let {
            viewModel.onNavigatedToCreated()
            onOpenProgram(it)
        }
    }
    TrainContent(
        uiState = uiState,
        onOpenLibrary = onOpenLibrary,
        onOpenTemplates = onOpenTemplates,
        onOpenProgram = onOpenProgram,
        onOpenDay = onOpenDay,
        onCreateBlank = viewModel::createBlankProgram,
        onOpenHistory = onOpenHistory,
        onStartEmpty = { viewModel.startEmptyWorkout(emptyName) },
    )
}

@Composable
internal fun TrainContent(
    uiState: TrainUiState,
    onOpenLibrary: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenProgram: (String) -> Unit,
    onOpenDay: (String) -> Unit,
    onCreateBlank: (String) -> Unit,
    onOpenHistory: () -> Unit = {},
    onStartEmpty: () -> Unit = {},
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_programs_title),
            subtitle = stringResource(R.string.feature_programs_subtitle),
        )
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val active = uiState.active
            if (active != null) {
                ActiveProgramCard(active, onOpenDay = onOpenDay, onOpenProgram = { onOpenProgram(active.program.id) })
            } else if (!uiState.isLoading) {
                NoProgramCard(onOpenTemplates)
            }

            SectionTitle(stringResource(R.string.feature_programs_my_programs), Modifier.padding(top = 8.dp))
            uiState.programs.forEach { program ->
                ProgramRow(program, isActive = program.id == active?.program?.id) { onOpenProgram(program.id) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JgSecondaryButton(onClick = onOpenTemplates, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_programs_from_template))
                }
                JgSecondaryButton(onClick = { naming = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_programs_blank_program))
                }
            }

            SectionTitle(stringResource(R.string.feature_programs_more), Modifier.padding(top = 8.dp))
            JgSecondaryButton(onClick = onStartEmpty, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_programs_start_empty))
            }
            EntryCard(
                title = stringResource(R.string.feature_programs_library_title),
                description = stringResource(R.string.feature_programs_library_desc),
                icon = JgIcons.Search,
                onClick = onOpenLibrary,
            )
            EntryCard(
                title = stringResource(R.string.feature_programs_history_title),
                description = stringResource(R.string.feature_programs_history_desc),
                icon = JgIcons.Progress,
                onClick = onOpenHistory,
            )
        }
    }
    if (naming) {
        TextInputDialog(
            title = stringResource(R.string.feature_programs_new_program),
            initial = stringResource(R.string.feature_programs_default_program_name),
            confirmLabel = stringResource(R.string.feature_programs_create),
            onConfirm = {
                naming = false
                onCreateBlank(it)
            },
            onDismiss = { naming = false },
        )
    }
}

@Composable
private fun ActiveProgramCard(active: ActiveProgram, onOpenDay: (String) -> Unit, onOpenProgram: () -> Unit) {
    val next = active.nextDay
    Card(
        onClick = onOpenProgram,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_programs_active).uppercase(), style = MaterialTheme.typography.labelMedium)
            Text(active.program.name, style = MaterialTheme.typography.headlineMedium)
            if (next != null) {
                Text(
                    stringResource(
                        R.string.feature_programs_next_day,
                        next.name,
                        pluralStringResource(R.plurals.feature_programs_exercise_count, next.exercises.size, next.exercises.size),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                JgPrimaryButton(onClick = { onOpenDay(next.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_programs_view_day, next.name))
                }
            } else {
                Text(stringResource(R.string.feature_programs_no_days), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun NoProgramCard(onOpenTemplates: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_programs_none_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feature_programs_none_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            JgPrimaryButton(onClick = onOpenTemplates, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_programs_browse_templates))
            }
        }
    }
}

@Composable
private fun ProgramRow(program: Program, isActive: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(program.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    program.days.joinToString(" · ") { it.name }.ifEmpty { stringResource(R.string.feature_programs_no_days) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            if (isActive) AssistChip(onClick = onClick, label = { Text(stringResource(R.string.feature_programs_active)) })
        }
    }
}

@Preview
@Composable
private fun TrainPreview() {
    JalilgymTheme {
        TrainContent(TrainUiState(isLoading = false), {}, {}, {}, {}, {})
    }
}
