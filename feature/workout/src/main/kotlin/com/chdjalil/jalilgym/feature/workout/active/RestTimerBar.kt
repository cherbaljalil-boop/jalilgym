package com.chdjalil.jalilgym.feature.workout.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import com.chdjalil.jalilgym.feature.workout.R
import kotlinx.coroutines.delay

private const val REST_STEP_SECONDS = 15

/** Big countdown with ±15 s and skip. The alert itself comes from the workout service. */
@Composable
internal fun RestTimerBar(rest: RestTimerState, onEvent: (ActiveWorkoutEvent) -> Unit) {
    val remaining by produceState(rest.remainingMillis(System.currentTimeMillis()), rest) {
        while (true) {
            value = rest.remainingMillis(System.currentTimeMillis())
            delay(200)
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(
                progress = { if (rest.totalMillis > 0) remaining / rest.totalMillis.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.feature_workout_rest),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatDuration(remaining + 999),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (rest.label.isNotEmpty()) {
                        Text(
                            rest.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                FilledTonalButton(onClick = { onEvent(ActiveWorkoutEvent.AdjustRest(-REST_STEP_SECONDS)) }) {
                    Text(stringResource(R.string.feature_workout_minus_seconds, REST_STEP_SECONDS))
                }
                FilledTonalButton(onClick = { onEvent(ActiveWorkoutEvent.AdjustRest(REST_STEP_SECONDS)) }) {
                    Text(stringResource(R.string.feature_workout_plus_seconds, REST_STEP_SECONDS))
                }
                TextButton(onClick = { onEvent(ActiveWorkoutEvent.SkipRest) }) {
                    Text(stringResource(R.string.feature_workout_skip))
                }
            }
        }
    }
}
