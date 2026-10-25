package com.chdjalil.jalilgym.feature.progress.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.designsystem.theme.JgTheme
import com.chdjalil.jalilgym.core.domain.progress.WEEKLY_SET_TARGET
import com.chdjalil.jalilgym.core.domain.progress.toHeat
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.TrainingStreak
import com.chdjalil.jalilgym.core.ui.EntryCard
import com.chdjalil.jalilgym.core.ui.StreakCard
import com.chdjalil.jalilgym.core.ui.body.BodyHeatmap
import com.chdjalil.jalilgym.core.ui.chart.ChartPoint
import com.chdjalil.jalilgym.core.ui.chart.LineChart
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.progress.R
import com.chdjalil.jalilgym.feature.progress.ui.NumberInputDialog
import com.chdjalil.jalilgym.feature.progress.ui.SectionCard
import com.chdjalil.jalilgym.feature.progress.ui.formatDate
import com.chdjalil.jalilgym.feature.progress.ui.formatWeight
import com.chdjalil.jalilgym.feature.progress.ui.formatWeightChange
import java.time.LocalDate
import java.time.ZoneId

internal fun LocalDate.toChartX(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
internal fun ProgressScreen(
    onOpenStrength: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenPhotos: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressContent(
        uiState = uiState,
        onPreviousWeek = viewModel::previousWeek,
        onNextWeek = viewModel::nextWeek,
        onLogWeight = viewModel::logWeight,
        onOpenStrength = onOpenStrength,
        onOpenRecords = onOpenRecords,
        onOpenBody = onOpenBody,
        onOpenPhotos = onOpenPhotos,
    )
}

@Composable
internal fun ProgressContent(
    uiState: ProgressUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onLogWeight: (Double) -> Unit,
    onOpenStrength: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var logWeight by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_progress_title),
            subtitle = stringResource(R.string.feature_progress_subtitle),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.streak?.let { StreakCard(it) }
            uiState.week?.let { WeeklyVolumeCard(it, onPreviousWeek, onNextWeek) }
            BodyWeightCard(uiState, onLog = { logWeight = true }, onOpen = onOpenBody)
            EntryCard(
                title = stringResource(R.string.feature_progress_strength_title),
                description = stringResource(R.string.feature_progress_strength_desc),
                icon = JgIcons.Progress,
                onClick = onOpenStrength,
            )
            EntryCard(
                title = stringResource(R.string.feature_progress_records_title),
                description = stringResource(R.string.feature_progress_records_desc),
                icon = JgIcons.Train,
                onClick = onOpenRecords,
            )
            EntryCard(
                title = stringResource(R.string.feature_progress_photos_title),
                description = stringResource(R.string.feature_progress_photos_desc),
                icon = JgIcons.AddPhoto,
                onClick = onOpenPhotos,
            )
        }
    }
    if (logWeight) {
        NumberInputDialog(
            title = stringResource(R.string.feature_progress_log_weight),
            label = uiState.units.weightSymbol,
            onDismiss = { logWeight = false },
            onConfirm = {
                logWeight = false
                onLogWeight(it)
            },
        )
    }
}

@Composable
private fun WeeklyVolumeCard(week: WeekVolume, onPrevious: () -> Unit, onNext: () -> Unit) {
    SectionCard(
        title = stringResource(R.string.feature_progress_volume_title),
        action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_progress_previous_week))
                }
                IconButton(onClick = onNext, enabled = !week.isCurrentWeek) {
                    Icon(
                        painterResource(JgIcons.ArrowBack),
                        stringResource(R.string.feature_progress_next_week),
                        modifier = Modifier.graphicsLayer { rotationZ = 180f },
                    )
                }
            }
        },
    ) {
        Text(
            if (week.isCurrentWeek) {
                stringResource(R.string.feature_progress_this_week)
            } else {
                stringResource(R.string.feature_progress_week_of, formatDate(week.weekStart))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BodyHeatmap(
            intensities = week.sets.toHeat(),
            contentDescription = stringResource(R.string.feature_progress_volume_heatmap_desc),
            modifier = Modifier.fillMaxWidth(),
        )
        if (week.sets.isEmpty()) {
            Text(
                stringResource(R.string.feature_progress_volume_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(
                    R.string.feature_progress_volume_target,
                    WEEKLY_SET_TARGET.start.toInt(),
                    WEEKLY_SET_TARGET.endInclusive.toInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            week.sets.entries.sortedByDescending { it.value }.forEach { (muscle, sets) ->
                val color = when {
                    sets in WEEKLY_SET_TARGET -> JgTheme.extendedColors.success
                    sets > WEEKLY_SET_TARGET.endInclusive -> JgTheme.extendedColors.warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(muscle.labelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.feature_progress_sets_value, formatNumber(sets)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyWeightCard(uiState: ProgressUiState, onLog: () -> Unit, onOpen: () -> Unit) {
    val units = uiState.units
    SectionCard(
        title = stringResource(R.string.feature_progress_body_weight),
        action = { TextButton(onClick = onLog) { Text(stringResource(R.string.feature_progress_log)) } },
    ) {
        val latest = uiState.latestWeight
        if (latest == null) {
            Text(
                stringResource(R.string.feature_progress_weight_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(units.formatWeight(latest.weightKg), style = MaterialTheme.typography.headlineSmall)
            Text(
                uiState.weightChangeKg?.let {
                    stringResource(R.string.feature_progress_weight_change, units.formatWeightChange(it))
                } ?: stringResource(R.string.feature_progress_logged_on, formatDate(latest.date)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.recentWeights.size >= 2) {
                LineChart(
                    points = uiState.recentWeights.map { ChartPoint(it.date.toChartX(), it.weightKg) },
                    trend = uiState.weightTrend.map { ChartPoint(it.first.toChartX(), it.second) },
                    contentDescription = stringResource(R.string.feature_progress_weight_chart_desc),
                    formatY = units::formatWeight,
                    height = 120.dp,
                )
            }
        }
        TextButton(onClick = onOpen) { Text(stringResource(R.string.feature_progress_body_open)) }
    }
}

@Preview
@Composable
private fun ProgressPreview() {
    JalilgymTheme {
        ProgressContent(
            uiState = ProgressUiState(isLoading = false, streak = TrainingStreak(3, 5, 1, 3, null)),
            onPreviousWeek = {},
            onNextWeek = {},
            onLogWeight = {},
            onOpenStrength = {},
            onOpenRecords = {},
            onOpenBody = {},
            onOpenPhotos = {},
        )
    }
}
