package com.chdjalil.jalilgym.feature.exercises.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseFilter
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.ui.body.BodyHeatmap
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.exercises.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseFilterSheet(
    filter: ExerciseFilter,
    resultCount: Int,
    onEvent: (ExerciseLibraryEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.feature_exercises_filters),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                TextButton(onClick = { onEvent(ExerciseLibraryEvent.FiltersCleared) }) {
                    Text(stringResource(R.string.feature_exercises_clear_all))
                }
            }

            SectionTitle(R.string.feature_exercises_filter_muscle)
            Text(
                stringResource(R.string.feature_exercises_filter_muscle_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BodyHeatmap(
                intensities = filter.muscles.associateWith { 1f },
                contentDescription = stringResource(R.string.feature_exercises_heatmap_picker_description),
                onMuscleClick = { onEvent(ExerciseLibraryEvent.MuscleToggled(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 48.dp),
            )
            ChipGroup(Muscle.entries, filter.muscles, { it.labelRes }) {
                onEvent(ExerciseLibraryEvent.MuscleToggled(it))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.feature_exercises_include_secondary),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = filter.includeSecondary,
                    onCheckedChange = { onEvent(ExerciseLibraryEvent.IncludeSecondaryChanged(it)) },
                )
            }

            SectionTitle(R.string.feature_exercises_filter_equipment)
            ChipGroup(Equipment.entries, filter.equipment, { it.labelRes }) {
                onEvent(ExerciseLibraryEvent.EquipmentToggled(it))
            }

            SectionTitle(R.string.feature_exercises_filter_type)
            ChipGroup(Mechanics.entries, filter.mechanics, { it.labelRes }) {
                onEvent(ExerciseLibraryEvent.MechanicsToggled(it))
            }

            SectionTitle(R.string.feature_exercises_filter_level)
            ChipGroup(Difficulty.entries, filter.difficulty, { it.labelRes }) {
                onEvent(ExerciseLibraryEvent.DifficultyToggled(it))
            }

            JgPrimaryButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.feature_exercises_show_results, resultCount, resultCount))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChipGroup(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> Int,
    onToggle: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(stringResource(label(option))) },
            )
        }
    }
}
