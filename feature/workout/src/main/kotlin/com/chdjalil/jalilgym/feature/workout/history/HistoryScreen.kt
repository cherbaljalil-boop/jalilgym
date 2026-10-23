package com.chdjalil.jalilgym.feature.workout.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.feature.workout.R
import com.chdjalil.jalilgym.feature.workout.active.formatDuration
import com.chdjalil.jalilgym.feature.workout.summary.SessionBody
import com.chdjalil.jalilgym.feature.workout.summary.SessionUiState
import com.chdjalil.jalilgym.feature.workout.summary.WorkoutSummaryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_workout_back))
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

internal fun formatDateTime(millis: Long): String =
    dateFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@Composable
internal fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState, onBack, onOpenSession)
}

@Composable
internal fun HistoryContent(uiState: HistoryUiState, onBack: () -> Unit, onOpenSession: (String) -> Unit) {
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_workout_history_title), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.sessions.isEmpty() -> Text(
                    stringResource(R.string.feature_workout_history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(uiState.sessions, key = { it.id }) { item ->
                        val units = uiState.units
                        ListItem(
                            modifier = Modifier.clickable { onOpenSession(item.id) },
                            overlineContent = { Text(formatDateTime(item.startedAt)) },
                            headlineContent = { Text(item.name, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string.feature_workout_history_row,
                                        formatDuration(item.endedAt - item.startedAt),
                                        "${formatNumber(units.fromKg(item.totalVolumeKg))} ${units.weightSymbol}",
                                        item.completedSets,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SessionDetailScreen(onBack: () -> Unit, viewModel: WorkoutSummaryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            BackBar(
                title = (uiState as? SessionUiState.Ready)?.summary?.session?.startedAt?.let(::formatDateTime).orEmpty(),
                onBack = onBack,
            ) {
                if (uiState is SessionUiState.Ready) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_workout_delete_session))
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
            when (val state = uiState) {
                SessionUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                SessionUiState.NotFound -> Text(stringResource(R.string.feature_workout_session_missing), Modifier.align(Alignment.Center))
                is SessionUiState.Ready -> SessionBody(state.summary, state.units)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_workout_delete_session_title)) },
            text = { Text(stringResource(R.string.feature_workout_delete_session_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.feature_workout_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_workout_cancel)) } },
        )
    }
}
