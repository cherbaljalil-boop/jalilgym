package com.chdjalil.jalilgym.feature.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar

@Composable
internal fun CustomFoodScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: CustomFoodViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.savedFoodId) { uiState.savedFoodId?.let(onSaved) }
    CustomFoodContent(uiState, onBack, viewModel::update, viewModel::save)
}

@Composable
internal fun CustomFoodContent(
    uiState: CustomFoodUiState,
    onBack: () -> Unit,
    onUpdate: (CustomFoodForm.() -> CustomFoodForm) -> Unit,
    onSave: () -> Unit,
) {
    val form = uiState.form
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_nutrition_create_food), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Field(R.string.feature_nutrition_food_name, form.name, text = true) { v -> onUpdate { copy(name = v) } }
            Field(R.string.feature_nutrition_brand_optional, form.brand, text = true) { v -> onUpdate { copy(brand = v) } }
            Field(R.string.feature_nutrition_barcode_optional, form.barcode, keyboard = KeyboardType.Number) { v ->
                onUpdate { copy(barcode = v.filter(Char::isDigit)) }
            }
            Text(
                stringResource(R.string.feature_nutrition_values_per_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(R.string.feature_nutrition_values_per, form.basisGrams, Modifier.weight(1f)) { v -> onUpdate { copy(basisGrams = v) } }
                Field(R.string.feature_nutrition_serving_name, form.servingLabel, Modifier.weight(1f), text = true) { v ->
                    onUpdate { copy(servingLabel = v) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(R.string.feature_nutrition_kcal_label, form.kcal, Modifier.weight(1f)) { v -> onUpdate { copy(kcal = v) } }
                Field(R.string.feature_nutrition_protein_g, form.protein, Modifier.weight(1f)) { v -> onUpdate { copy(protein = v) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(R.string.feature_nutrition_carbs_g, form.carbs, Modifier.weight(1f)) { v -> onUpdate { copy(carbs = v) } }
                Field(R.string.feature_nutrition_fat_g, form.fat, Modifier.weight(1f)) { v -> onUpdate { copy(fat = v) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(R.string.feature_nutrition_fiber_g, form.fiber, Modifier.weight(1f)) { v -> onUpdate { copy(fiber = v) } }
                Field(R.string.feature_nutrition_sugar_g, form.sugar, Modifier.weight(1f)) { v -> onUpdate { copy(sugar = v) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(R.string.feature_nutrition_sat_fat_g, form.satFat, Modifier.weight(1f)) { v -> onUpdate { copy(satFat = v) } }
                Field(R.string.feature_nutrition_sodium_mg, form.sodiumMg, Modifier.weight(1f)) { v -> onUpdate { copy(sodiumMg = v) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = form.markedHalal, onCheckedChange = { checked -> onUpdate { copy(markedHalal = checked) } })
                Text(stringResource(R.string.feature_nutrition_mark_halal_own), style = MaterialTheme.typography.bodyMedium)
            }
            JgPrimaryButton(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_nutrition_save_food))
            }
        }
    }
}

@Composable
private fun Field(
    label: Int,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Decimal,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (text) KeyboardType.Text else keyboard),
        modifier = modifier,
    )
}
