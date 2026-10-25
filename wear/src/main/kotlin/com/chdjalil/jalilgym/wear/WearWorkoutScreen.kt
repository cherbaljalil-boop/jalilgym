package com.chdjalil.jalilgym.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.model.UnitSystem
import kotlinx.coroutines.delay

data class WearActions(
    val onWeight: (Int) -> Unit = {},
    val onReps: (Int) -> Unit = {},
    val onDone: () -> Unit = {},
    val onSkipRest: () -> Unit = {},
)

@Composable
fun WearWorkoutScreen(state: WearUiState, actions: WearActions, clock: () -> Long = System::currentTimeMillis) {
    val workout = state.workout
    var now by remember { mutableLongStateOf(clock()) }
    val restEndsAt = workout?.restEndsAt
    LaunchedEffect(restEndsAt) {
        while (restEndsAt != null && clock() < restEndsAt) {
            now = clock()
            delay(1_000)
        }
        now = clock()
    }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = rememberScalingLazyListState(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            workout == null -> item { Message(stringResource(R.string.wear_connecting)) }
            !workout.active -> {
                item { Title(stringResource(R.string.wear_app_name)) }
                item { Message(stringResource(R.string.wear_start_on_phone)) }
            }
            else -> {
                item {
                    Text(
                        stringResource(R.string.wear_progress, workout.completedSets, workout.totalSets),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (restEndsAt != null && now < restEndsAt) {
                    item { Rest(restEndsAt - now, workout.restLabel, actions.onSkipRest) }
                }
                val draft = state.draft
                if (draft == null) {
                    item { Message(stringResource(R.string.wear_all_done)) }
                } else {
                    item { Title(draft.set.exerciseName) }
                    item {
                        Text(
                            stringResource(
                                if (draft.set.isWarmup) R.string.wear_set_warmup else R.string.wear_set,
                                draft.set.number,
                                draft.set.count,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (draft.tracksWeight) {
                        item {
                            Stepper(
                                value = weightText(draft.weight ?: 0.0, draft.units),
                                lessLabel = stringResource(R.string.wear_less_weight),
                                moreLabel = stringResource(R.string.wear_more_weight),
                                onChange = actions.onWeight,
                            )
                        }
                    }
                    item {
                        Stepper(
                            value = repsText(draft.reps, draft.set.tracking),
                            lessLabel = stringResource(R.string.wear_fewer_reps),
                            moreLabel = stringResource(R.string.wear_more_reps),
                            onChange = actions.onReps,
                        )
                    }
                    item {
                        Button(onClick = actions.onDone, enabled = !state.sending, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(if (state.sending) R.string.wear_saving else R.string.wear_done),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                if (state.phoneUnreachable) {
                    item {
                        Text(
                            stringResource(R.string.wear_phone_unreachable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 2)
}

@Composable
private fun Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun weightText(weight: Double, units: UnitSystem): String =
    stringResource(if (units == UnitSystem.IMPERIAL) R.string.wear_weight_lb else R.string.wear_weight_kg, formatNumber(weight))

@Composable
private fun repsText(reps: Int, tracking: TrackingType): String = stringResource(
    when (tracking) {
        TrackingType.TIME -> R.string.wear_seconds
        TrackingType.DISTANCE -> R.string.wear_meters
        else -> R.string.wear_reps
    },
    reps,
)

@Composable
private fun Stepper(value: String, lessLabel: String, moreLabel: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(onClick = { onChange(-1) }, modifier = Modifier.size(44.dp).semantics { contentDescription = lessLabel }) {
            Text("−")
        }
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 2.dp))
        FilledTonalButton(onClick = { onChange(1) }, modifier = Modifier.size(44.dp).semantics { contentDescription = moreLabel }) {
            Text("+")
        }
    }
}

@Composable
private fun Rest(remainingMillis: Long, label: String?, onSkip: () -> Unit) {
    val seconds = ((remainingMillis + 999) / 1_000).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.wear_rest, seconds / 60, seconds % 60),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        label?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 2)
        }
        FilledTonalButton(onClick = onSkip) { Text(stringResource(R.string.wear_skip_rest)) }
    }
}
