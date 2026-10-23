package com.chdjalil.jalilgym.feature.progress.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.ui.chart.ChartPoint
import com.chdjalil.jalilgym.core.ui.chart.LineChart
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.progress.R
import com.chdjalil.jalilgym.feature.progress.hub.toChartX
import com.chdjalil.jalilgym.feature.progress.ui.BackBar
import com.chdjalil.jalilgym.feature.progress.ui.NumberInputDialog
import com.chdjalil.jalilgym.feature.progress.ui.SectionCard
import com.chdjalil.jalilgym.feature.progress.ui.formatDate
import com.chdjalil.jalilgym.feature.progress.ui.formatMeasurement
import com.chdjalil.jalilgym.feature.progress.ui.formatSigned
import com.chdjalil.jalilgym.feature.progress.ui.formatWeight
import com.chdjalil.jalilgym.feature.progress.ui.fromCm
import com.chdjalil.jalilgym.feature.progress.ui.measurementSymbol

/** How many weigh-ins the history list shows. */
private const val WEIGHT_HISTORY_ROWS = 10

@Composable
internal fun BodyScreen(onBack: () -> Unit, viewModel: BodyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BodyContent(
        uiState = uiState,
        onBack = onBack,
        onLogWeight = { viewModel.logWeight(it) },
        onDeleteWeight = viewModel::deleteWeight,
        onLogMeasurement = { type, value -> viewModel.logMeasurement(type, value) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BodyContent(
    uiState: BodyUiState,
    onBack: () -> Unit,
    onLogWeight: (Double) -> Unit,
    onDeleteWeight: (String) -> Unit,
    onLogMeasurement: (MeasurementType, Double) -> Unit,
) {
    val units = uiState.units
    var logWeight by rememberSaveable { mutableStateOf(false) }
    var measuring by rememberSaveable { mutableStateOf<MeasurementType?>(null) }
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_progress_body_title), onBack) },
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
            SectionCard(
                title = stringResource(R.string.feature_progress_body_weight),
                action = { TextButton(onClick = { logWeight = true }) { Text(stringResource(R.string.feature_progress_log)) } },
            ) {
                if (uiState.weights.isEmpty()) {
                    Text(
                        stringResource(R.string.feature_progress_weight_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    if (uiState.weights.size >= 2) {
                        LineChart(
                            points = uiState.weights.map { ChartPoint(it.date.toChartX(), it.weightKg) },
                            trend = uiState.weightTrend.map { ChartPoint(it.first.toChartX(), it.second) },
                            contentDescription = stringResource(R.string.feature_progress_weight_chart_desc),
                            formatY = units::formatWeight,
                        )
                        Text(
                            stringResource(R.string.feature_progress_trend_legend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    uiState.weights.asReversed().take(WEIGHT_HISTORY_ROWS).forEach { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDate(entry.date), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(units.formatWeight(entry.weightKg), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDeleteWeight(entry.id) }) {
                                Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_progress_delete))
                            }
                        }
                    }
                }
            }
            SectionCard(title = stringResource(R.string.feature_progress_measurements)) {
                Text(
                    stringResource(R.string.feature_progress_measurements_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasurementType.entries.forEach { type ->
                        FilterChip(selected = false, onClick = { measuring = type }, label = { Text(stringResource(type.labelRes)) })
                    }
                }
                uiState.measurements.forEach { summary ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(summary.type.labelRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                formatDate(summary.latest.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(units.formatMeasurement(summary.type, summary.latest.value), style = MaterialTheme.typography.titleMedium)
                            summary.change?.let { change ->
                                val shown = units.fromCm(summary.type, change)
                                Text(
                                    formatSigned(shown) + " " + units.measurementSymbol(summary.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (logWeight) {
        NumberInputDialog(
            title = stringResource(R.string.feature_progress_log_weight),
            label = units.weightSymbol,
            onDismiss = { logWeight = false },
            onConfirm = {
                logWeight = false
                onLogWeight(it)
            },
        )
    }
    measuring?.let { type ->
        NumberInputDialog(
            title = stringResource(R.string.feature_progress_log_measurement, stringResource(type.labelRes)),
            label = units.measurementSymbol(type),
            onDismiss = { measuring = null },
            onConfirm = {
                measuring = null
                onLogMeasurement(type, it)
            },
        )
    }
}
