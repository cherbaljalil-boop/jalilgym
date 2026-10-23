package com.chdjalil.jalilgym.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.ui.ComingSoonCard
import com.chdjalil.jalilgym.core.ui.EntryCard

@Composable
internal fun TodayScreen(
    onOpenDay: (String) -> Unit,
    onChooseProgram: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayContent(uiState, onOpenDay, onChooseProgram, onSkip = viewModel::skipToNextDay)
}

@Composable
internal fun TodayContent(
    uiState: TodayUiState,
    onOpenDay: (String) -> Unit,
    onChooseProgram: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_today_title),
            subtitle = stringResource(R.string.feature_today_subtitle),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val active = uiState.active
            when {
                active?.nextDay != null -> TodayWorkoutCard(active, onOpenDay, onSkip)
                !uiState.isLoading -> EntryCard(
                    title = stringResource(R.string.feature_today_choose_program_title),
                    description = stringResource(R.string.feature_today_choose_program_desc),
                    icon = JgIcons.Train,
                    onClick = onChooseProgram,
                )
            }
            ComingSoonCard(
                title = stringResource(R.string.feature_today_macros_title),
                description = stringResource(R.string.feature_today_macros_desc),
                phase = 5,
                icon = JgIcons.Nutrition,
            )
            ComingSoonCard(
                title = stringResource(R.string.feature_today_streak_title),
                description = stringResource(R.string.feature_today_streak_desc),
                phase = 4,
                icon = JgIcons.Progress,
            )
            uiState.exerciseCount?.let { count ->
                Text(
                    pluralStringResource(R.plurals.feature_today_exercises_ready, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TodayWorkoutCard(active: ActiveProgram, onOpenDay: (String) -> Unit, onSkip: () -> Unit) {
    val day = active.nextDay ?: return
    Card(
        onClick = { onOpenDay(day.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.feature_today_todays_workout).uppercase(),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(day.name.uppercase(), style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(
                    R.string.feature_today_workout_summary,
                    active.program.name,
                    pluralStringResource(R.plurals.feature_today_exercise_count, day.exercises.size, day.exercises.size),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JgPrimaryButton(onClick = { onOpenDay(day.id) }, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(JgIcons.Play), contentDescription = null)
                    Text(stringResource(R.string.feature_today_view_workout), Modifier.padding(start = 8.dp))
                }
                if (active.program.days.size > 1) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.feature_today_skip_day)) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TodayPreview() {
    JalilgymTheme { TodayContent(TodayUiState(isLoading = false, exerciseCount = 99), {}, {}, {}) }
}
