package com.chdjalil.jalilgym.feature.progress.strength

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.ui.chart.ChartPoint
import com.chdjalil.jalilgym.core.ui.chart.LineChart
import com.chdjalil.jalilgym.feature.progress.R
import com.chdjalil.jalilgym.feature.progress.records.RecordRow
import com.chdjalil.jalilgym.feature.progress.ui.BackBar
import com.chdjalil.jalilgym.feature.progress.ui.SectionCard
import com.chdjalil.jalilgym.feature.progress.ui.formatDate
import com.chdjalil.jalilgym.feature.progress.ui.formatWeight
import com.chdjalil.jalilgym.feature.progress.ui.formatWeightChange

@Composable
internal fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
        )
    }
}

@Composable
internal fun StrengthListScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    viewModel: StrengthListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_progress_strength_title), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                uiState.exercises.isEmpty() -> EmptyMessage(stringResource(R.string.feature_progress_strength_empty))
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(uiState.exercises, key = { it.exercise.id }) { item ->
                        ListItem(
                            modifier = Modifier.clickable { onOpenExercise(item.exercise.id) },
                            headlineContent = { Text(item.exercise.name, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.feature_progress_sessions_last,
                                        item.sessionCount,
                                        item.sessionCount,
                                        formatDate(item.lastPerformedAt),
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                val best = item.bestEstimatedOneRepMaxKg
                                if (best != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(uiState.units.formatWeight(best), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            stringResource(R.string.feature_progress_e1rm_short),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
        }
    }
}

private val StrengthMetric.labelRes: Int
    get() = when (this) {
        StrengthMetric.EST_1RM -> R.string.feature_progress_metric_e1rm
        StrengthMetric.TOP_WEIGHT -> R.string.feature_progress_metric_top_weight
        StrengthMetric.VOLUME -> R.string.feature_progress_metric_volume
    }

private val ChartRange.labelRes: Int
    get() = when (this) {
        ChartRange.WEEKS_4 -> R.string.feature_progress_range_4w
        ChartRange.MONTHS_3 -> R.string.feature_progress_range_3m
        ChartRange.MONTHS_6 -> R.string.feature_progress_range_6m
        ChartRange.YEAR -> R.string.feature_progress_range_1y
        ChartRange.ALL -> R.string.feature_progress_range_all
    }

@Composable
internal fun ExerciseProgressScreen(onBack: () -> Unit, viewModel: ExerciseProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseProgressContent(uiState, onBack, viewModel::selectMetric, viewModel::selectRange)
}

@Composable
internal fun ExerciseProgressContent(
    uiState: ExerciseProgressUiState,
    onBack: () -> Unit,
    onSelectMetric: (StrengthMetric) -> Unit,
    onSelectRange: (ChartRange) -> Unit,
) {
    Scaffold(
        topBar = { BackBar(uiState.exercise?.name.orEmpty(), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrengthMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = metric == uiState.metric,
                        onClick = { onSelectMetric(metric) },
                        label = { Text(stringResource(metric.labelRes)) },
                    )
                }
            }
            SectionCard(title = stringResource(uiState.metric.labelRes)) {
                when {
                    uiState.points.isEmpty() -> Text(
                        stringResource(R.string.feature_progress_chart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> {
                        val units = uiState.units
                        val first = uiState.points.first().second
                        val last = uiState.points.last().second
                        Text(units.formatWeight(last), style = MaterialTheme.typography.headlineSmall)
                        if (uiState.points.size >= 2) {
                            Text(
                                stringResource(R.string.feature_progress_change_in_range, units.formatWeightChange(last - first)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LineChart(
                            points = uiState.points.map { ChartPoint(it.first, it.second) },
                            contentDescription = stringResource(
                                R.string.feature_progress_strength_chart_desc,
                                stringResource(uiState.metric.labelRes),
                                uiState.exercise?.name.orEmpty(),
                            ),
                            formatY = units::formatWeight,
                        )
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartRange.entries.forEach { range ->
                        FilterChip(
                            selected = range == uiState.range,
                            onClick = { onSelectRange(range) },
                            label = { Text(stringResource(range.labelRes)) },
                        )
                    }
                }
            }
            Text(
                pluralStringResource(R.plurals.feature_progress_sessions, uiState.sessionCount, uiState.sessionCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.records.isNotEmpty()) {
                SectionCard(title = stringResource(R.string.feature_progress_records_title)) {
                    uiState.records.forEach { RecordRow(it, uiState.units, showExercise = false) }
                }
            }
        }
    }
}
