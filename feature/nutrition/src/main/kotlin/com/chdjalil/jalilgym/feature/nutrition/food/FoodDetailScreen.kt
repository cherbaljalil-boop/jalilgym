package com.chdjalil.jalilgym.feature.nutrition.food

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.FoodServing
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.HalalBadge
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGrams
import com.chdjalil.jalilgym.feature.nutrition.ui.formatKcal
import com.chdjalil.jalilgym.feature.nutrition.ui.labelRes

@Composable
internal fun FoodDetailScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: FoodDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.done) { if (uiState.done) onDone() }
    FoodDetailContent(
        uiState = uiState,
        onBack = onBack,
        onAmount = viewModel::setAmountText,
        onServing = viewModel::selectServing,
        onMeal = viewModel::selectMeal,
        onToggleFavorite = viewModel::toggleFavorite,
        onHalalMark = viewModel::setHalalMark,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FoodDetailContent(
    uiState: FoodDetailUiState,
    onBack: () -> Unit,
    onAmount: (String) -> Unit = {},
    onServing: (FoodServing?) -> Unit = {},
    onMeal: (MealType) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onHalalMark: (Boolean) -> Unit = {},
    onSave: () -> Unit = {},
) {
    val food = uiState.food
    Scaffold(
        topBar = {
            BackBar(food?.name.orEmpty(), onBack) {
                if (food != null) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            painterResource(if (food.isFavorite) JgIcons.Check else JgIcons.Add),
                            stringResource(if (food.isFavorite) R.string.feature_nutrition_unfavorite else R.string.feature_nutrition_favorite),
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (food == null || uiState.isLoading) {
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
            food.brand?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HalalBadge(food.halalStatus)

            val n = uiState.nutrients
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.feature_nutrition_kcal_value, n?.let { formatKcal(it.kcal) } ?: "—"),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    NutrientRows(n)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.amount.text,
                    onValueChange = onAmount,
                    label = { Text(stringResource(R.string.feature_nutrition_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                uiState.grams?.let { Text(stringResource(R.string.feature_nutrition_grams, formatGrams(it))) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.amount.serving == null,
                    onClick = { onServing(null) },
                    label = { Text(stringResource(R.string.feature_nutrition_unit_grams)) },
                )
                food.servings.forEach { serving ->
                    FilterChip(
                        selected = uiState.amount.serving == serving,
                        onClick = { onServing(serving) },
                        label = { Text(stringResource(R.string.feature_nutrition_serving_label, serving.label, formatGrams(serving.grams))) },
                    )
                }
            }
            if (!uiState.isEditing) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { meal ->
                        FilterChip(selected = uiState.meal == meal, onClick = { onMeal(meal) }, label = { Text(stringResource(meal.labelRes)) })
                    }
                }
            }
            JgPrimaryButton(onClick = onSave, enabled = uiState.grams != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (uiState.isEditing) R.string.feature_nutrition_update else R.string.feature_nutrition_log_food))
            }

            when (food.halalStatus) {
                HalalStatus.UNKNOWN -> TextButton(onClick = { onHalalMark(true) }) {
                    Text(stringResource(R.string.feature_nutrition_mark_halal))
                }
                HalalStatus.USER_MARKED -> TextButton(onClick = { onHalalMark(false) }) {
                    Text(stringResource(R.string.feature_nutrition_unmark_halal))
                }
                else -> Unit
            }
            if (food.source == FoodSource.OPEN_FOOD_FACTS) {
                Text(
                    stringResource(R.string.feature_nutrition_off_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NutrientRows(n: Nutrients?) {
    @Composable
    fun row(label: Int, value: Double?, unit: String, indent: Boolean = false) {
        Row(Modifier.fillMaxWidth().padding(start = if (indent) 16.dp else 0.dp)) {
            Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value?.let { "${formatGrams(it)} $unit" } ?: "—", style = MaterialTheme.typography.bodyMedium)
        }
    }
    row(R.string.feature_nutrition_protein, n?.proteinG, "g")
    row(R.string.feature_nutrition_carbs, n?.carbsG, "g")
    row(R.string.feature_nutrition_fiber, n?.fiberG, "g", indent = true)
    row(R.string.feature_nutrition_sugar, n?.sugarG, "g", indent = true)
    row(R.string.feature_nutrition_fat, n?.fatG, "g")
    row(R.string.feature_nutrition_sat_fat, n?.satFatG, "g", indent = true)
    HorizontalDivider()
    row(R.string.feature_nutrition_sodium, n?.sodiumMg, "mg")
}
