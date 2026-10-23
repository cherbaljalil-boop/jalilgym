package com.chdjalil.jalilgym.feature.exercises.detail

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.units.formatWeightValue
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.ui.ExerciseImage
import com.chdjalil.jalilgym.core.ui.body.BodyHeatmap
import com.chdjalil.jalilgym.core.ui.body.muscleTargetIntensities
import com.chdjalil.jalilgym.core.ui.chart.ChartPoint
import com.chdjalil.jalilgym.core.ui.chart.LineChart
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.exercises.R
import kotlinx.coroutines.delay

@Composable
internal fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onBack() }
    ExerciseDetailContent(uiState, onBack, onEdit, onDelete = viewModel::deleteExercise, history = history)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseDetailContent(
    uiState: ExerciseDetailUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    history: ExerciseHistoryUiState = ExerciseHistoryUiState(),
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val exercise = (uiState as? ExerciseDetailUiState.Success)?.exercise

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_exercises_back))
                    }
                },
                actions = {
                    if (exercise != null && !exercise.isBuiltIn) {
                        IconButton(onClick = { onEdit(exercise.id) }) {
                            Icon(painterResource(JgIcons.Edit), stringResource(R.string.feature_exercises_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_exercises_delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
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
                ExerciseDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ExerciseDetailUiState.NotFound -> Text(
                    stringResource(R.string.feature_exercises_not_found),
                    modifier = Modifier.align(Alignment.Center),
                )
                is ExerciseDetailUiState.Success -> ExerciseDetailBody(uiState.exercise, history)
            }
        }
    }

    if (confirmDelete && exercise != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_exercises_delete_title, exercise.name)) },
            text = { Text(stringResource(R.string.feature_exercises_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.feature_exercises_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_exercises_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailBody(exercise: Exercise, history: ExerciseHistoryUiState) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DemoImages(exercise)
        Text(exercise.name.uppercase(), style = MaterialTheme.typography.headlineLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                exercise.equipment.labelRes,
                exercise.mechanics.labelRes,
                exercise.difficulty.labelRes,
                exercise.trackingType.labelRes,
            ).forEach { label -> SuggestionChip(onClick = {}, label = { Text(stringResource(label)) }) }
        }

        SectionTitle(R.string.feature_exercises_muscles)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BodyHeatmap(
                intensities = muscleTargetIntensities(exercise.primaryMuscles, exercise.secondaryMuscles),
                contentDescription = stringResource(
                    R.string.feature_exercises_heatmap_description,
                    exercise.primaryMuscles.labels(),
                ),
                modifier = Modifier
                    .width(150.dp)
                    .height(150.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MuscleLegend(R.string.feature_exercises_primary, exercise.primaryMuscles)
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    MuscleLegend(R.string.feature_exercises_secondary, exercise.secondaryMuscles)
                }
            }
        }

        if (exercise.instructions.isNotEmpty()) {
            SectionTitle(R.string.feature_exercises_how_to)
            exercise.instructions.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        SectionTitle(R.string.feature_exercises_history_title)
        HistorySection(history)
    }
}

@Composable
private fun HistorySection(history: ExerciseHistoryUiState) {
    val units = history.units
    fun weight(kg: Double) = "${units.formatWeightValue(kg)} ${units.weightSymbol}"
    if (history.sessions.isEmpty()) {
        Text(
            stringResource(R.string.feature_exercises_history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Text(
        pluralStringResource(R.plurals.feature_exercises_history_sessions, history.sessions.size, history.sessions.size),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    history.bests.forEach { record ->
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(record.type.labelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(weight(record.value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
    val points = history.sessions.mapNotNull { p -> p.bestEstimatedOneRepMaxKg?.let { ChartPoint(p.performedAt, it) } }
    if (points.size >= 2) {
        LineChart(
            points = points,
            contentDescription = stringResource(R.string.feature_exercises_history_chart),
            formatY = ::weight,
            height = 140.dp,
        )
    }
}

/** Alternates the start and end frames to suggest the movement. */
@Composable
private fun DemoImages(exercise: Exercise) {
    var frame by rememberSaveable(exercise.id) { mutableIntStateOf(0) }
    val frames = exercise.images
    if (frames.size > 1) {
        LaunchedEffect(exercise.id) {
            while (true) {
                delay(1_200)
                frame = (frame + 1) % frames.size
            }
        }
    }
    val description = stringResource(com.chdjalil.jalilgym.core.ui.R.string.core_ui_exercise_image, exercise.name)
    Crossfade(targetState = frames.getOrNull(frame), label = "demo") { path ->
        ExerciseImage(
            path = path,
            contentDescription = description,
            placeholderIconSize = 48.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .clip(MaterialTheme.shapes.large),
        )
    }
}

@Composable
private fun List<Muscle>.labels(): String = map { stringResource(it.labelRes) }.joinToString(", ")

@Composable
private fun MuscleLegend(title: Int, muscles: List<Muscle>) {
    Column {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(muscles.labels(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview
@Composable
private fun DetailPreview() {
    JalilgymTheme {
        ExerciseDetailContent(ExerciseDetailUiState.NotFound, onBack = {}, onEdit = {}, onDelete = {})
    }
}
