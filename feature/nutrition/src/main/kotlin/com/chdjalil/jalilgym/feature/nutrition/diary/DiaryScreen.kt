package com.chdjalil.jalilgym.feature.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JgTheme
import com.chdjalil.jalilgym.core.domain.nutrition.CheckIn
import com.chdjalil.jalilgym.core.domain.nutrition.CheckInVerdict
import com.chdjalil.jalilgym.core.model.FoodLogEntry
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.ui.EntryCard
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.CalorieRing
import com.chdjalil.jalilgym.feature.nutrition.ui.MacroBar
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGrams
import com.chdjalil.jalilgym.feature.nutrition.ui.formatKcal
import com.chdjalil.jalilgym.feature.nutrition.ui.labelRes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Navigation out of the diary; dates travel as ISO strings. */
data class DiaryActions(
    val onAddFood: (LocalDate, MealType) -> Unit = { _, _ -> },
    val onScan: (LocalDate, MealType) -> Unit = { _, _ -> },
    val onEditEntry: (FoodLogEntry) -> Unit = {},
    val onOpenSavedMeals: (LocalDate, MealType) -> Unit = { _, _ -> },
    val onOpenSetup: () -> Unit = {},
)

@Composable
internal fun DiaryScreen(actions: DiaryActions, viewModel: DiaryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is DiaryMessage.Copied ->
                    resources.getQuantityString(R.plurals.feature_nutrition_copied, message.count, message.count)
                DiaryMessage.NothingToCopy -> resources.getString(R.string.feature_nutrition_nothing_to_copy)
                DiaryMessage.MealSaved -> resources.getString(R.string.feature_nutrition_meal_saved)
                DiaryMessage.NothingToSave -> resources.getString(R.string.feature_nutrition_nothing_to_save)
            }
            snackbar.showSnackbar(text)
        }
    }
    Box(Modifier.fillMaxSize()) {
        DiaryContent(
            uiState = uiState,
            actions = actions,
            onPreviousDay = viewModel::previousDay,
            onNextDay = viewModel::nextDay,
            onToday = viewModel::goToToday,
            onQuickAdd = viewModel::quickAdd,
            onDeleteEntry = viewModel::deleteEntry,
            onCopyFromPreviousDay = viewModel::copyFromPreviousDay,
            onSaveMeal = viewModel::saveMeal,
            onApplyCheckIn = viewModel::applyCheckIn,
            onDismissCheckIn = viewModel::dismissCheckIn,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
internal fun DiaryContent(
    uiState: DiaryUiState,
    actions: DiaryActions,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onToday: () -> Unit = {},
    onQuickAdd: (MealType, String, Nutrients) -> Unit = { _, _, _ -> },
    onDeleteEntry: (String) -> Unit = {},
    onCopyFromPreviousDay: (MealType) -> Unit = {},
    onSaveMeal: (MealType, String) -> Unit = { _, _ -> },
    onApplyCheckIn: () -> Unit = {},
    onDismissCheckIn: () -> Unit = {},
) {
    var quickAddMeal by rememberSaveable { mutableStateOf<MealType?>(null) }
    var saveMealFor by rememberSaveable { mutableStateOf<MealType?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_nutrition_title),
            subtitle = stringResource(R.string.feature_nutrition_subtitle),
        )
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateBar(uiState, onPreviousDay, onNextDay, onToday)
            if (!uiState.isLoading && uiState.plan == null) {
                EntryCard(
                    title = stringResource(R.string.feature_nutrition_setup_title),
                    description = stringResource(R.string.feature_nutrition_setup_prompt),
                    icon = JgIcons.Nutrition,
                    onClick = actions.onOpenSetup,
                )
            }
            uiState.checkIn?.takeIf { uiState.isToday }?.let { CheckInCard(it, onApplyCheckIn, onDismissCheckIn) }
            SummaryCard(uiState, actions.onOpenSetup)
            MealType.entries.forEach { meal ->
                MealSection(
                    meal = meal,
                    entries = uiState.day.entriesFor(meal),
                    onAdd = { actions.onAddFood(uiState.date, meal) },
                    onScan = { actions.onScan(uiState.date, meal) },
                    onEdit = actions.onEditEntry,
                    onDelete = onDeleteEntry,
                    onQuickAdd = { quickAddMeal = meal },
                    onCopy = { onCopyFromPreviousDay(meal) },
                    onSavedMeals = { actions.onOpenSavedMeals(uiState.date, meal) },
                    onSaveMeal = { saveMealFor = meal },
                )
            }
            Text(
                stringResource(R.string.feature_nutrition_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    quickAddMeal?.let { meal ->
        QuickAddDialog(
            meal = meal,
            onDismiss = { quickAddMeal = null },
            onConfirm = { name, nutrients ->
                quickAddMeal = null
                onQuickAdd(meal, name, nutrients)
            },
        )
    }
    saveMealFor?.let { meal ->
        SaveMealDialog(
            defaultName = stringResource(meal.labelRes),
            onDismiss = { saveMealFor = null },
            onConfirm = { name ->
                saveMealFor = null
                onSaveMeal(meal, name)
            },
        )
    }
}

@Composable
private fun DateBar(uiState: DiaryUiState, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_nutrition_previous_day))
        }
        Text(
            if (uiState.isToday) stringResource(R.string.feature_nutrition_today) else dayFormat.format(uiState.date),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    enabled = !uiState.isToday,
                    onClickLabel = stringResource(R.string.feature_nutrition_go_to_today),
                    role = Role.Button,
                    onClick = onToday,
                ),
        )
        if (uiState.loggingStreak > 0) {
            Text(
                pluralStringResource(R.plurals.feature_nutrition_logging_streak, uiState.loggingStreak, uiState.loggingStreak),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                painterResource(JgIcons.ArrowBack),
                stringResource(R.string.feature_nutrition_next_day),
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
            )
        }
    }
}

@Composable
private fun SummaryCard(uiState: DiaryUiState, onOpenTargets: () -> Unit) {
    val totals = uiState.day.totals
    val targets = uiState.plan?.targets
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CalorieRing(eaten = totals.kcal, target = targets?.calories)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroBar(stringResource(R.string.feature_nutrition_protein), totals.proteinG, targets?.proteinG, MaterialTheme.colorScheme.primary)
                    MacroBar(stringResource(R.string.feature_nutrition_carbs), totals.carbsG, targets?.carbsG, MaterialTheme.colorScheme.tertiary)
                    MacroBar(stringResource(R.string.feature_nutrition_fat), totals.fatG, targets?.fatG, JgTheme.extendedColors.warning)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Micro(stringResource(R.string.feature_nutrition_fiber), totals.fiberG, "g")
                Micro(stringResource(R.string.feature_nutrition_sugar), totals.sugarG, "g")
                Micro(stringResource(R.string.feature_nutrition_sat_fat), totals.satFatG, "g")
                Micro(stringResource(R.string.feature_nutrition_sodium), totals.sodiumMg, "mg")
            }
            if (targets != null) {
                TextButton(onClick = onOpenTargets) { Text(stringResource(R.string.feature_nutrition_edit_targets)) }
            }
        }
    }
}

@Composable
private fun CheckInCard(checkIn: CheckIn, onApply: () -> Unit, onDismiss: () -> Unit) {
    val change = checkIn.weeklyChangeKg ?: return
    val percent = checkIn.weeklyChangePercent ?: return
    val locale = LocalConfiguration.current.locales[0]
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.feature_nutrition_check_in_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(
                    R.string.feature_nutrition_check_in_change,
                    "%+.1f".format(locale, change),
                    "%+.1f".format(locale, percent),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    when (checkIn.verdict) {
                        CheckInVerdict.TOO_SLOW -> R.string.feature_nutrition_check_in_too_slow
                        CheckInVerdict.TOO_FAST -> R.string.feature_nutrition_check_in_too_fast
                        else -> R.string.feature_nutrition_check_in_on_track
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (checkIn.adjustmentKcal != 0) {
                    JgPrimaryButton(onClick = onApply) {
                        Text(
                            stringResource(
                                if (checkIn.adjustmentKcal > 0) {
                                    R.string.feature_nutrition_check_in_add
                                } else {
                                    R.string.feature_nutrition_check_in_remove
                                },
                                kotlin.math.abs(checkIn.adjustmentKcal),
                            ),
                        )
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_nutrition_check_in_keep)) }
                } else {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_nutrition_check_in_ok)) }
                }
            }
        }
    }
}

@Composable
private fun Micro(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let { "${formatGrams(it)} $unit" } ?: "—", style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MealSection(
    meal: MealType,
    entries: List<FoodLogEntry>,
    onAdd: () -> Unit,
    onScan: () -> Unit,
    onEdit: (FoodLogEntry) -> Unit,
    onDelete: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onCopy: () -> Unit,
    onSavedMeals: () -> Unit,
    onSaveMeal: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val mealName = stringResource(meal.labelRes)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mealName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(
                        stringResource(R.string.feature_nutrition_kcal_value, formatKcal(entries.sumOf { it.nutrients.kcal })),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onScan) {
                    Icon(painterResource(JgIcons.Barcode), stringResource(R.string.feature_nutrition_scan_for, mealName))
                }
                IconButton(onClick = onAdd) {
                    Icon(painterResource(JgIcons.Add), stringResource(R.string.feature_nutrition_add_to, mealName))
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(painterResource(JgIcons.More), stringResource(R.string.feature_nutrition_more_for, mealName))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feature_nutrition_quick_add)) },
                            onClick = {
                                menu = false
                                onQuickAdd()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feature_nutrition_copy_previous)) },
                            onClick = {
                                menu = false
                                onCopy()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feature_nutrition_saved_meals)) },
                            onClick = {
                                menu = false
                                onSavedMeals()
                            },
                        )
                        if (entries.any { it.foodId != null }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feature_nutrition_save_as_meal)) },
                                onClick = {
                                    menu = false
                                    onSaveMeal()
                                },
                            )
                        }
                    }
                }
            }
            entries.forEach { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = entry.foodId != null,
                            onClickLabel = stringResource(R.string.feature_nutrition_edit_amount),
                        ) { onEdit(entry) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (entry.quantityG > 0) {
                                stringResource(R.string.feature_nutrition_entry_detail, formatGrams(entry.quantityG), formatKcal(entry.nutrients.kcal))
                            } else {
                                stringResource(R.string.feature_nutrition_kcal_value, formatKcal(entry.nutrients.kcal))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(entry.id) }) {
                        Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_nutrition_remove_entry, entry.name))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddDialog(meal: MealType, onDismiss: () -> Unit, onConfirm: (String, Nutrients) -> Unit) {
    val defaultName = stringResource(R.string.feature_nutrition_quick_add)
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    fun num(text: String) = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
    val kcalValue = num(kcal)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_nutrition_quick_add_to, stringResource(meal.labelRes))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.feature_nutrition_name_optional)) }, singleLine = true)
                listOf(
                    Triple(kcal, { v: String -> kcal = v }, R.string.feature_nutrition_kcal_label),
                    Triple(protein, { v: String -> protein = v }, R.string.feature_nutrition_protein_g),
                    Triple(carbs, { v: String -> carbs = v }, R.string.feature_nutrition_carbs_g),
                    Triple(fat, { v: String -> fat = v }, R.string.feature_nutrition_fat_g),
                ).forEach { (value, onChange, label) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(stringResource(label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = kcalValue != null,
                onClick = {
                    onConfirm(
                        name.trim().ifEmpty { defaultName },
                        Nutrients(kcalValue ?: 0.0, num(protein) ?: 0.0, num(carbs) ?: 0.0, num(fat) ?: 0.0),
                    )
                },
            ) { Text(stringResource(R.string.feature_nutrition_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_nutrition_cancel)) } },
    )
}

@Composable
private fun SaveMealDialog(defaultName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_nutrition_save_as_meal)) },
        text = {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.feature_nutrition_meal_name)) }, singleLine = true)
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) {
                Text(stringResource(R.string.feature_nutrition_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_nutrition_cancel)) } },
    )
}
