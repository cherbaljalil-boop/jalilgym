package com.chdjalil.jalilgym.feature.workout.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.ui.ExerciseImage
import com.chdjalil.jalilgym.feature.workout.R

@Composable
internal fun ExerciseCard(
    exercise: SessionExercise,
    inSuperset: Boolean,
    units: UnitSystem,
    effortScale: EffortScale,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    onSwap: () -> Unit,
    onPlates: () -> Unit,
    onOneRepMax: () -> Unit,
    onWarmups: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseImage(
                    path = exercise.exercise.images.firstOrNull(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    if (inSuperset) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Dot(MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.feature_workout_superset).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                    exercise.target?.let { target ->
                        val reps = if (target.repMin == target.repMax) "${target.repMin}" else "${target.repMin}–${target.repMax}"
                        Text(
                            stringResource(
                                R.string.feature_workout_target,
                                target.targetSets,
                                reps,
                                formatDuration(target.restSeconds * 1000L),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ExerciseMenu(onSwap, onPlates, onOneRepMax, onWarmups) {
                    onEvent(ActiveWorkoutEvent.RemoveExercise(exercise.id))
                }
            }

            val repsLabel = when (exercise.exercise.trackingType) {
                TrackingType.TIME -> stringResource(R.string.feature_workout_col_seconds)
                TrackingType.DISTANCE -> stringResource(R.string.feature_workout_col_meters)
                TrackingType.WEIGHT_REPS, TrackingType.REPS -> stringResource(R.string.feature_workout_col_reps)
            }
            SetHeader(units, effortScale, repsLabel)
            var workingNumber = 0
            exercise.sets.forEachIndexed { index, set ->
                if (set.type == SetType.WORKING) workingNumber++
                SetRow(
                    set = set,
                    workingNumber = if (set.type == SetType.WORKING) workingNumber else index + 1,
                    previous = exercise.previous.getOrNull(index),
                    units = units,
                    effortScale = effortScale,
                    onEdit = { transform -> onEvent(ActiveWorkoutEvent.EditSet(set.id, transform)) },
                    onCycleType = { onEvent(ActiveWorkoutEvent.CycleType(set)) },
                    onToggleComplete = { onEvent(ActiveWorkoutEvent.ToggleComplete(set)) },
                    onDelete = { onEvent(ActiveWorkoutEvent.RemoveSet(set.id)) },
                )
            }
            TextButton(onClick = { onEvent(ActiveWorkoutEvent.AddSet(exercise.id)) }, modifier = Modifier.padding(start = 4.dp)) {
                Icon(painterResource(JgIcons.Add), contentDescription = null)
                Text(stringResource(R.string.feature_workout_add_set), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ExerciseMenu(
    onSwap: () -> Unit,
    onPlates: () -> Unit,
    onOneRepMax: () -> Unit,
    onWarmups: () -> Unit,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(JgIcons.More), stringResource(R.string.feature_workout_exercise_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                R.string.feature_workout_add_warmups to onWarmups,
                R.string.feature_workout_plates_title to onPlates,
                R.string.feature_workout_one_rm_title to onOneRepMax,
                R.string.feature_workout_swap_exercise to onSwap,
                R.string.feature_workout_remove_exercise to onRemove,
            ).forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}
