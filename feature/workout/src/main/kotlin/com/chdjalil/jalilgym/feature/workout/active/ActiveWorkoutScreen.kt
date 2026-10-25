package com.chdjalil.jalilgym.feature.workout.active

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.component.JgSecondaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.feature.workout.R
import com.chdjalil.jalilgym.feature.workout.service.WorkoutService
import com.chdjalil.jalilgym.feature.workout.tools.OneRepMaxSheet
import com.chdjalil.jalilgym.feature.workout.tools.PlateCalculatorSheet
import com.chdjalil.jalilgym.feature.workout.tools.WarmupSheet
import kotlinx.coroutines.delay

@Composable
internal fun ActiveWorkoutScreen(
    onMinimize: () -> Unit,
    onPickExercises: () -> Unit,
    onFinished: (String) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val finished by viewModel.finishedSessionId.collectAsStateWithLifecycle()
    val discarded by viewModel.discarded.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val restLabel = stringResource(R.string.feature_workout_rest_label)
    SideEffect {
        viewModel.restLabel = { name, set, total -> restLabel.format(name, set, total) }
    }
    LaunchedEffect(finished) { finished?.let(onFinished) }
    LaunchedEffect(discarded) { if (discarded) onDiscarded() }

    val active = uiState as? ActiveWorkoutUiState.Active
    // The service keeps the timer and controls alive outside the app; it stops itself when the workout ends.
    LaunchedEffect(active != null) { if (active != null) WorkoutService.start(context) }
    NotificationPermissionRequest()
    KeepScreenOn(active?.preferences?.keepScreenOn == true)

    ActiveWorkoutContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onMinimize = onMinimize,
        onAddExercise = {
            viewModel.onEvent(ActiveWorkoutEvent.RequestAdd)
            onPickExercises()
        },
        onSwapExercise = {
            viewModel.onEvent(ActiveWorkoutEvent.RequestSwap(it))
            onPickExercises()
        },
    )
}

/** Asks once for notification permission (Android 13+) so the rest timer can alert. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** Which tool or dialog is open, and for which exercise. */
private sealed interface WorkoutDialog {
    data class Plates(val exercise: SessionExercise) : WorkoutDialog

    data class OneRepMax(val exercise: SessionExercise) : WorkoutDialog

    data class Warmups(val exercise: SessionExercise) : WorkoutDialog

    data object Finish : WorkoutDialog

    data object Discard : WorkoutDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    onMinimize: () -> Unit,
    onAddExercise: () -> Unit,
    onSwapExercise: (String) -> Unit,
) {
    var dialog by remember { mutableStateOf<WorkoutDialog?>(null) }
    val active = uiState as? ActiveWorkoutUiState.Active

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(painterResource(JgIcons.ArrowDown), stringResource(R.string.feature_workout_minimize))
                    }
                },
                title = {
                    if (active != null) {
                        Column {
                            Text(active.session.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            ElapsedTime(active.session.startedAt)
                        }
                    }
                },
                actions = {
                    if (active != null) {
                        TextButton(onClick = { dialog = WorkoutDialog.Finish }, modifier = Modifier.testTag("finish_workout")) {
                            Text(stringResource(R.string.feature_workout_finish), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { active?.rest?.let { RestTimerBar(it, onEvent) } },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (uiState) {
                ActiveWorkoutUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ActiveWorkoutUiState.None -> Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.feature_workout_none), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onMinimize) { Text(stringResource(R.string.feature_workout_close)) }
                }
                is ActiveWorkoutUiState.Active -> WorkoutList(
                    state = uiState,
                    onEvent = onEvent,
                    onAddExercise = onAddExercise,
                    onSwapExercise = onSwapExercise,
                    onOpenTool = { dialog = it },
                    onDiscard = { dialog = WorkoutDialog.Discard },
                )
            }
        }
    }

    val units = active?.preferences?.units
    when (val current = dialog) {
        null -> Unit
        is WorkoutDialog.Plates -> PlateCalculatorSheet(units!!, current.exercise.workingWeightKg()) { dialog = null }
        is WorkoutDialog.OneRepMax -> OneRepMaxSheet(
            units!!,
            current.exercise.workingWeightKg(),
            current.exercise.sets.firstOrNull { it.reps != null }?.reps,
        ) { dialog = null }
        is WorkoutDialog.Warmups -> WarmupSheet(
            units!!,
            current.exercise.workingWeightKg(),
            onInsert = {
                onEvent(ActiveWorkoutEvent.AddWarmups(current.exercise.id, it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        WorkoutDialog.Finish -> FinishDialog(
            session = active?.session,
            onConfirm = {
                onEvent(ActiveWorkoutEvent.Finish(it))
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        WorkoutDialog.Discard -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.feature_workout_discard_title)) },
            text = { Text(stringResource(R.string.feature_workout_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(ActiveWorkoutEvent.Discard)
                    dialog = null
                }) { Text(stringResource(R.string.feature_workout_discard)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.feature_workout_keep_going)) } },
        )
    }
}

/** Heaviest entered working weight, used to pre-fill the tools. */
private fun SessionExercise.workingWeightKg(): Double? =
    sets.filter { it.type == SetType.WORKING }.mapNotNull { it.weightKg }.maxOrNull()

@Composable
private fun ElapsedTime(startedAt: Long) {
    val elapsed by produceState(System.currentTimeMillis() - startedAt, startedAt) {
        while (true) {
            value = System.currentTimeMillis() - startedAt
            delay(1_000)
        }
    }
    Text(
        formatDuration(elapsed),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun WorkoutList(
    state: ActiveWorkoutUiState.Active,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    onAddExercise: () -> Unit,
    onSwapExercise: (String) -> Unit,
    onOpenTool: (WorkoutDialog) -> Unit,
    onDiscard: () -> Unit,
) {
    val exercises = state.session.exercises
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("workout_list"),
    ) {
        items(exercises, key = { it.id }) { exercise ->
            val index = exercises.indexOf(exercise)
            val inSuperset = exercise.supersetGroup != null && (
                exercises.getOrNull(index - 1)?.supersetGroup == exercise.supersetGroup ||
                    exercises.getOrNull(index + 1)?.supersetGroup == exercise.supersetGroup
                )
            ExerciseCard(
                exercise = exercise,
                inSuperset = inSuperset,
                units = state.preferences.units,
                effortScale = state.preferences.effortScale,
                onEvent = onEvent,
                onSwap = { onSwapExercise(exercise.id) },
                onPlates = { onOpenTool(WorkoutDialog.Plates(exercise)) },
                onOneRepMax = { onOpenTool(WorkoutDialog.OneRepMax(exercise)) },
                onWarmups = { onOpenTool(WorkoutDialog.Warmups(exercise)) },
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (exercises.isEmpty()) {
                    Text(
                        stringResource(R.string.feature_workout_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                JgSecondaryButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(JgIcons.Add), contentDescription = null)
                    Text(stringResource(R.string.feature_workout_add_exercise), Modifier.padding(start = 8.dp))
                }
                TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_workout_discard_workout), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Shows the column titles once per exercise card. */
@Composable
internal fun SetHeader(units: UnitSystem, effortScale: EffortScale, repsLabel: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val style = MaterialTheme.typography.labelSmall
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            stringResource(R.string.feature_workout_col_set),
            style = style,
            color = color,
            modifier = Modifier.width(TYPE_COLUMN),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.feature_workout_col_previous),
            style = style,
            color = color,
            modifier = Modifier.width(PREVIOUS_COLUMN),
            textAlign = TextAlign.Center,
        )
        Text(units.weightSymbol.uppercase(), style = style, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(repsLabel.uppercase(), style = style, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (effortScale != EffortScale.OFF) {
            Text(
                stringResource(if (effortScale == EffortScale.RPE) R.string.feature_workout_rpe else R.string.feature_workout_rir),
                style = style,
                color = color,
                modifier = Modifier.width(EFFORT_COLUMN),
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.width(CHECK_COLUMN))
    }
}

@Composable
private fun FinishDialog(session: WorkoutSession?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var notes by rememberSaveable { mutableStateOf("") }
    val incomplete = session?.exercises?.sumOf { e -> e.sets.count { !it.completed } } ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_workout_finish_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (incomplete > 0) {
                    Text(
                        pluralStringResource(R.plurals.feature_workout_unchecked_sets, incomplete, incomplete),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.feature_workout_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            JgPrimaryButton(onClick = { onConfirm(notes) }, modifier = Modifier.testTag("confirm_finish")) {
                Text(stringResource(R.string.feature_workout_finish))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_workout_keep_going)) } },
    )
}

/** 0:42, 12:05, 1:02:33. */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
