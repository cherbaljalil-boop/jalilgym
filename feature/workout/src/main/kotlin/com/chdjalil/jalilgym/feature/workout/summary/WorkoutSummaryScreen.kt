package com.chdjalil.jalilgym.feature.workout.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.theme.JgTheme
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.formatWeightValue
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.ProgressionNote
import com.chdjalil.jalilgym.core.model.ProgressionOutcome
import com.chdjalil.jalilgym.core.model.RecordNote
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.WorkoutSummary
import com.chdjalil.jalilgym.core.ui.achievementText
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.workout.R
import com.chdjalil.jalilgym.feature.workout.active.formatDuration

@Composable
internal fun WorkoutSummaryScreen(onDone: () -> Unit, viewModel: WorkoutSummaryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Box(Modifier.weight(1f)) {
            when (val state = uiState) {
                SessionUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                SessionUiState.NotFound -> Text(stringResource(R.string.feature_workout_session_missing), Modifier.align(Alignment.Center))
                is SessionUiState.Ready -> SessionBody(state.summary, state.units, header = {
                    Text(
                        stringResource(R.string.feature_workout_summary_title).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                })
            }
        }
        JgPrimaryButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) { Text(stringResource(R.string.feature_workout_done)) }
    }
}

/** Stats, progression notes and every logged set. Shared with the history detail page. */
@Composable
internal fun SessionBody(summary: WorkoutSummary, units: UnitSystem, header: @Composable () -> Unit = {}) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                header()
                Text(summary.session.name, style = MaterialTheme.typography.headlineMedium)
                summary.session.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(stringResource(R.string.feature_workout_stat_duration), formatDuration(summary.durationMillis), Modifier.weight(1f))
                Stat(
                    stringResource(R.string.feature_workout_stat_volume),
                    "${formatNumber(units.fromKg(summary.totalVolumeKg))} ${units.weightSymbol}",
                    Modifier.weight(1f),
                )
                Stat(stringResource(R.string.feature_workout_stat_sets), "${summary.completedSets}", Modifier.weight(1f))
            }
        }
        if (summary.records.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.feature_workout_new_records),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(summary.records) { RecordNoteRow(it, units) }
        }
        if (summary.newAchievements.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.feature_workout_new_badges),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(summary.newAchievements) { id ->
                val (title, description) = achievementText(id)
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (summary.progression.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.feature_workout_next_time),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(summary.progression) { ProgressionRow(it, units) }
        }
        items(summary.session.exercises, key = { it.id }) { ExerciseSets(it, units) }
    }
}

@Composable
private fun RecordNoteRow(note: RecordNote, units: UnitSystem) {
    val previous = note.previousValue ?: return
    Column {
        Text(
            stringResource(R.string.feature_workout_record_title, note.exerciseName, stringResource(note.type.labelRes)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(
                R.string.feature_workout_record_change,
                units.formatWeightValue(previous),
                units.formatWeightValue(note.value),
                units.weightSymbol,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = JgTheme.extendedColors.success,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProgressionRow(note: ProgressionNote, units: UnitSystem) {
    val unit = units.weightSymbol
    val from = units.formatWeightValue(note.fromWeightKg)
    val to = units.formatWeightValue(note.toWeightKg)
    val (text, color) = when (note.outcome) {
        ProgressionOutcome.INCREASE ->
            stringResource(R.string.feature_workout_progress_increase, from, to, unit) to JgTheme.extendedColors.success
        ProgressionOutcome.HOLD ->
            stringResource(R.string.feature_workout_progress_hold, to, unit, note.nextTargetReps) to MaterialTheme.colorScheme.onSurface
        ProgressionOutcome.MISSED ->
            stringResource(R.string.feature_workout_progress_missed, to, unit) to JgTheme.extendedColors.warning
        ProgressionOutcome.DELOAD ->
            stringResource(R.string.feature_workout_progress_deload, from, to, unit) to JgTheme.extendedColors.warning
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(note.exerciseName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun ExerciseSets(exercise: SessionExercise, units: UnitSystem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
            exercise.sets.filter { it.completed }.forEach { set ->
                val weight = set.weightKg?.let { "${units.formatWeightValue(it)} ${units.weightSymbol} × " }.orEmpty()
                val type = when (set.type) {
                    SetType.WARMUP -> " · " + stringResource(R.string.feature_workout_set_warmup)
                    SetType.DROP -> " · " + stringResource(R.string.feature_workout_set_drop)
                    SetType.FAILURE -> " · " + stringResource(R.string.feature_workout_set_failure)
                    SetType.WORKING -> ""
                }
                Text(
                    "$weight${set.reps ?: 0}$type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
