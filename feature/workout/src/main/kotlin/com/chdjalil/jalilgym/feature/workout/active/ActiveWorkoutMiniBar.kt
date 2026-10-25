package com.chdjalil.jalilgym.feature.workout.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.feature.workout.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MiniBarState(val name: String, val startedAt: Long, val rest: RestTimerState?)

@HiltViewModel
class ActiveWorkoutBarViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    restTimer: RestTimer,
) : ViewModel() {
    val state: StateFlow<MiniBarState?> = combine(workoutRepository.observeActiveSession(), restTimer.state) { session, rest ->
        session?.let { MiniBarState(it.name, it.startedAt, rest) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** "Workout in progress" strip shown above the tab bar while a workout runs in the background. */
@Composable
fun ActiveWorkoutMiniBar(onOpen: () -> Unit, viewModel: ActiveWorkoutBarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    val now by produceState(System.currentTimeMillis(), current) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(JgIcons.Play), contentDescription = null)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(current.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    current.rest?.let {
                        stringResource(R.string.feature_workout_mini_resting, formatDuration(it.remainingMillis(now) + 999))
                    } ?: stringResource(R.string.feature_workout_mini_elapsed, formatDuration(now - current.startedAt)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(stringResource(R.string.feature_workout_resume), style = MaterialTheme.typography.labelLarge)
        }
    }
}
