package com.chdjalil.jalilgym.feature.nutrition.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.domain.nutrition.TargetClamp
import com.chdjalil.jalilgym.core.domain.nutrition.minimumCalories
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.descriptionRes
import com.chdjalil.jalilgym.feature.nutrition.ui.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SetupScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }
    SetupContent(
        uiState = uiState,
        onBack = onBack,
        onSex = viewModel::setSex,
        onBirthDate = viewModel::setBirthDate,
        onHeight = viewModel::setHeight,
        onWeight = viewModel::setWeight,
        onActivity = viewModel::setActivity,
        onGoal = viewModel::setGoal,
        onStyle = viewModel::setStyle,
        onHalalFilter = viewModel::setHalalFilter,
        onOverride = viewModel::setOverride,
        onClearOverride = viewModel::clearOverride,
        onSave = viewModel::save,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SetupContent(
    uiState: SetupUiState,
    onBack: () -> Unit,
    onSex: (Sex) -> Unit = {},
    onBirthDate: (LocalDate) -> Unit = {},
    onHeight: (String) -> Unit = {},
    onWeight: (String) -> Unit = {},
    onActivity: (ActivityLevel) -> Unit = {},
    onGoal: (DietGoal) -> Unit = {},
    onStyle: (DietStyle) -> Unit = {},
    onHalalFilter: (HalalFilter) -> Unit = {},
    onOverride: (TargetOverride) -> Unit = {},
    onClearOverride: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            BackBar(
                stringResource(if (uiState.isEditing) R.string.feature_nutrition_targets_title else R.string.feature_nutrition_setup_title),
                onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        // Answers from earlier load first, so edits can't be overwritten.
        if (uiState.isLoading) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.feature_nutrition_about_you))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { sex ->
                    FilterChip(
                        selected = uiState.sex == sex,
                        onClick = { onSex(sex) },
                        label = { Text(stringResource(if (sex == Sex.MALE) R.string.feature_nutrition_sex_male else R.string.feature_nutrition_sex_female)) },
                    )
                }
            }
            Text(
                stringResource(R.string.feature_nutrition_sex_why),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    uiState.birthDate?.let { stringResource(R.string.feature_nutrition_born_on, dateFormat.format(it)) }
                        ?: stringResource(R.string.feature_nutrition_pick_birth_date),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.height,
                    onValueChange = onHeight,
                    label = {
                        Text(stringResource(R.string.feature_nutrition_height, if (uiState.units == UnitSystem.METRIC) "cm" else "in"))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = uiState.weight,
                    onValueChange = onWeight,
                    label = { Text(stringResource(R.string.feature_nutrition_weight, uiState.units.weightSymbol)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle(stringResource(R.string.feature_nutrition_activity))
            ActivityLevel.entries.forEach { level ->
                FilterChip(
                    selected = uiState.activity == level,
                    onClick = { onActivity(level) },
                    label = { Text(stringResource(level.labelRes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionTitle(stringResource(R.string.feature_nutrition_goal))
            DietGoal.entries.forEach { goal ->
                FilterChip(
                    selected = uiState.goal == goal,
                    onClick = { onGoal(goal) },
                    label = {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(stringResource(goal.labelRes), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(goal.descriptionRes), style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DietStyle.entries.forEach { style ->
                    FilterChip(selected = uiState.style == style, onClick = { onStyle(style) }, label = { Text(stringResource(style.labelRes)) })
                }
            }

            SectionTitle(stringResource(R.string.feature_nutrition_halal_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HalalFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.halalFilter == filter,
                        onClick = { onHalalFilter(filter) },
                        label = { Text(stringResource(filter.labelRes)) },
                    )
                }
            }
            Text(
                stringResource(R.string.feature_nutrition_halal_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TargetsCard(uiState, onOverride, onClearOverride)

            JgPrimaryButton(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_nutrition_save_targets))
            }
            Text(
                stringResource(R.string.feature_nutrition_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.birthDate ?: LocalDate.now().minusYears(25))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = (LocalDate.now().year - 100)..(LocalDate.now().year - 13),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onBirthDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickDate = false
                }) { Text(stringResource(R.string.feature_nutrition_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.feature_nutrition_cancel)) } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun TargetsCard(uiState: SetupUiState, onOverride: (TargetOverride) -> Unit, onClearOverride: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(stringResource(R.string.feature_nutrition_your_targets))
            val computed = uiState.computed
            if (computed == null) {
                Text(
                    stringResource(R.string.feature_nutrition_targets_need_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Text(
                stringResource(R.string.feature_nutrition_tdee, computed.tdee),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            computed.clamps.forEach { clamp ->
                Text(
                    when (clamp) {
                        TargetClamp.CALORIE_FLOOR ->
                            stringResource(R.string.feature_nutrition_clamp_floor, minimumCalories(uiState.sex ?: Sex.FEMALE))
                        TargetClamp.WEEKLY_LOSS_CAP -> stringResource(R.string.feature_nutrition_clamp_weekly)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            val base = computed.targets
            val o = uiState.override
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetField(R.string.feature_nutrition_kcal_label, o.calories, base.calories, Modifier.weight(1f)) { onOverride(o.copy(calories = it)) }
                TargetField(R.string.feature_nutrition_protein_g, o.protein, base.proteinG, Modifier.weight(1f)) { onOverride(o.copy(protein = it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetField(R.string.feature_nutrition_carbs_g, o.carbs, base.carbsG, Modifier.weight(1f)) { onOverride(o.copy(carbs = it)) }
                TargetField(R.string.feature_nutrition_fat_g, o.fat, base.fatG, Modifier.weight(1f)) { onOverride(o.copy(fat = it)) }
            }
            Text(
                stringResource(R.string.feature_nutrition_override_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.hasOverride) {
                TextButton(onClick = onClearOverride) { Text(stringResource(R.string.feature_nutrition_use_computed)) }
            }
        }
    }
}

/** Empty means "use the computed value", shown as the placeholder. */
@Composable
private fun TargetField(label: Int, value: String, computed: Int, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(5)) },
        label = { Text(stringResource(label)) },
        placeholder = { Text("$computed") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
