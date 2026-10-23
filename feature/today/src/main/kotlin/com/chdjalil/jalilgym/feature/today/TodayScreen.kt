package com.chdjalil.jalilgym.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.ui.ComingSoonCard

@Composable
internal fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayContent(uiState)
}

@Composable
internal fun TodayContent(uiState: TodayUiState, modifier: Modifier = Modifier) {
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
            uiState.exerciseCount?.let { count ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.feature_today_exercises_ready, count),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            ComingSoonCard(
                title = stringResource(R.string.feature_today_workout_title),
                description = stringResource(R.string.feature_today_workout_desc),
                phase = 2,
                icon = JgIcons.Train,
            )
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
        }
    }
}

@Preview
@Composable
private fun TodayPreview() {
    JalilgymTheme { TodayContent(TodayUiState(exerciseCount = 18)) }
}
