package com.chdjalil.jalilgym.feature.programs.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.program.isLinkedToNext
import com.chdjalil.jalilgym.feature.programs.R
import com.chdjalil.jalilgym.feature.programs.ui.BackTopBar
import com.chdjalil.jalilgym.feature.programs.ui.ExerciseSlotRow

@Composable
internal fun DayPreviewScreen(
    onBack: () -> Unit,
    onEditDay: (String) -> Unit,
    viewModel: DayPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DayPreviewContent(uiState, onBack, onEditDay, onMakeNext = viewModel::makeNext)
}

@Composable
internal fun DayPreviewContent(
    uiState: DayPreviewUiState,
    onBack: () -> Unit,
    onEditDay: (String) -> Unit,
    onMakeNext: () -> Unit,
) {
    val ready = uiState as? DayPreviewUiState.Ready
    Scaffold(
        // Full-screen (no bottom bar), so this screen handles system bars itself.
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            BackTopBar(title = ready?.programName.orEmpty(), onBack = onBack) {
                if (ready != null) {
                    IconButton(onClick = { onEditDay(ready.day.id) }) {
                        Icon(painterResource(JgIcons.Edit), stringResource(R.string.feature_programs_edit_day))
                    }
                }
            }
        },
        bottomBar = {
            if (ready != null) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ready.canMakeNext) {
                        TextButton(onClick = onMakeNext, modifier = Modifier.fillMaxWidth()) {
                            Icon(painterResource(JgIcons.SkipNext), contentDescription = null)
                            Text(stringResource(R.string.feature_programs_make_next), Modifier.padding(start = 8.dp))
                        }
                    }
                    // Workout logging arrives in Phase 3; the button is shown so the flow is clear.
                    JgPrimaryButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(JgIcons.Play), contentDescription = null)
                        Text(stringResource(R.string.feature_programs_start_workout_soon), Modifier.padding(start = 8.dp))
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
                DayPreviewUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DayPreviewUiState.NotFound -> Text(stringResource(R.string.feature_programs_day_missing), Modifier.align(Alignment.Center))
                is DayPreviewUiState.Ready -> {
                    val day = uiState.day
                    val groups = day.exercises.map { it.supersetGroup }
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                if (uiState.isNext) {
                                    Text(
                                        stringResource(R.string.feature_programs_up_next).uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(day.name.uppercase(), style = MaterialTheme.typography.displaySmall)
                                Text(
                                    pluralStringResource(
                                        R.plurals.feature_programs_set_count,
                                        day.exercises.sumOf { it.targetSets },
                                        day.exercises.sumOf { it.targetSets },
                                    ) + " · " + pluralStringResource(
                                        R.plurals.feature_programs_exercise_count,
                                        day.exercises.size,
                                        day.exercises.size,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        itemsIndexed(day.exercises, key = { _, slot -> slot.id }) { index, slot ->
                            ExerciseSlotRow(
                                slot = slot,
                                inSuperset = groups.isLinkedToNext(index) || groups.isLinkedToNext(index - 1),
                            )
                        }
                    }
                }
            }
        }
    }
}
