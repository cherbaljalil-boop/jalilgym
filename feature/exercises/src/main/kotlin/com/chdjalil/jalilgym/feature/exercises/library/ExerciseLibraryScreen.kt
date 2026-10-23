package com.chdjalil.jalilgym.feature.exercises.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.ui.ExerciseImage
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.exercises.R

@Composable
internal fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onCreateExercise: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseLibraryContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onExerciseClick = onExerciseClick,
        onCreateExercise = onCreateExercise,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseLibraryContent(
    uiState: ExerciseLibraryUiState,
    onEvent: (ExerciseLibraryEvent) -> Unit,
    onBack: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onCreateExercise: () -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_exercises_library_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_exercises_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateExercise,
                icon = { Icon(painterResource(JgIcons.Add), contentDescription = null) },
                text = { Text(stringResource(R.string.feature_exercises_new_exercise)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        // The app scaffold already pads for system bars.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SearchField(query = uiState.filter.query, onQueryChange = { onEvent(ExerciseLibraryEvent.QueryChanged(it)) })
            ActiveFiltersRow(uiState, onEvent, onOpenFilters = { showFilters = true })
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.exercises.isEmpty() -> EmptyResults(onClear = {
                    onEvent(ExerciseLibraryEvent.QueryChanged(""))
                    onEvent(ExerciseLibraryEvent.FiltersCleared)
                })
                else -> ExerciseList(uiState.exercises, onExerciseClick)
            }
        }
    }

    if (showFilters) {
        ExerciseFilterSheet(
            filter = uiState.filter,
            resultCount = uiState.exercises.size,
            onEvent = onEvent,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("exercise_search"),
        placeholder = { Text(stringResource(R.string.feature_exercises_search_hint)) },
        leadingIcon = { Icon(painterResource(JgIcons.Search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(JgIcons.Close), stringResource(R.string.feature_exercises_clear_search))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun ActiveFiltersRow(
    uiState: ExerciseLibraryUiState,
    onEvent: (ExerciseLibraryEvent) -> Unit,
    onOpenFilters: () -> Unit,
) {
    val filter = uiState.filter
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilterChip(
                selected = filter.activeFilterCount > 0,
                onClick = onOpenFilters,
                label = {
                    Text(
                        if (filter.activeFilterCount > 0) {
                            stringResource(R.string.feature_exercises_filters_count, filter.activeFilterCount)
                        } else {
                            stringResource(R.string.feature_exercises_filters)
                        },
                    )
                },
                leadingIcon = { Icon(painterResource(JgIcons.Filter), null, Modifier.size(18.dp)) },
            )
        }
        items(filter.muscles.sorted()) { muscle ->
            RemovableChip(stringResource(muscle.labelRes)) { onEvent(ExerciseLibraryEvent.MuscleToggled(muscle)) }
        }
        items(filter.equipment.sorted()) { equipment ->
            RemovableChip(stringResource(equipment.labelRes)) { onEvent(ExerciseLibraryEvent.EquipmentToggled(equipment)) }
        }
        items(filter.mechanics.sorted()) { mechanics ->
            RemovableChip(stringResource(mechanics.labelRes)) { onEvent(ExerciseLibraryEvent.MechanicsToggled(mechanics)) }
        }
        items(filter.difficulty.sorted()) { difficulty ->
            RemovableChip(stringResource(difficulty.labelRes)) {
                onEvent(ExerciseLibraryEvent.DifficultyToggled(difficulty))
            }
        }
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.feature_exercises_result_count,
                    uiState.exercises.size,
                    uiState.exercises.size,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    val removeLabel = stringResource(R.string.feature_exercises_remove_filter, label)
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(painterResource(JgIcons.Close), removeLabel, Modifier.size(16.dp)) },
    )
}

@Composable
private fun ExerciseList(exercises: List<Exercise>, onExerciseClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.testTag("exercise_list"),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(exercises, key = { it.id }) { exercise ->
            ExerciseRow(exercise, onClick = { onExerciseClick(exercise.id) })
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    val muscles = exercise.primaryMuscles.map { stringResource(it.labelRes) }.joinToString(", ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(exercise.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                stringResource(R.string.feature_exercises_row_subtitle, muscles, stringResource(exercise.equipment.labelRes)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            ExerciseImage(
                path = exercise.images.firstOrNull(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        },
        trailingContent = if (!exercise.isBuiltIn) {
            { AssistChip(onClick = onClick, label = { Text(stringResource(R.string.feature_exercises_custom)) }) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun EmptyResults(onClear: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.feature_exercises_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feature_exercises_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClear) { Text(stringResource(R.string.feature_exercises_clear_all)) }
    }
}

@Preview
@Composable
private fun LibraryPreview() {
    JalilgymTheme {
        ExerciseLibraryContent(
            uiState = ExerciseLibraryUiState(isLoading = false),
            onEvent = {},
            onBack = {},
            onExerciseClick = {},
            onCreateExercise = {},
        )
    }
}
