package com.chdjalil.jalilgym.feature.nutrition.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.domain.hydration.WATER_GOAL_RANGE_ML
import com.chdjalil.jalilgym.core.domain.hydration.flOzToMl
import com.chdjalil.jalilgym.core.domain.hydration.mlToFlOz
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.nutrition.R
import kotlin.math.roundToInt

/** "1,250 ml" or "42 fl oz". */
@Composable
private fun waterAmount(ml: Int, units: UnitSystem): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (units) {
        UnitSystem.METRIC -> stringResource(R.string.feature_nutrition_water_ml, String.format(locale, "%,d", ml))
        UnitSystem.IMPERIAL -> stringResource(R.string.feature_nutrition_water_fl_oz, mlToFlOz(ml).roundToInt())
    }
}

@Composable
internal fun WaterCard(
    water: WaterUiState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onSetGoal: (Int?) -> Unit,
) {
    var editingGoal by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.feature_nutrition_water),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                Text(
                    stringResource(
                        R.string.feature_nutrition_water_progress,
                        waterAmount(water.totalMl, water.units),
                        waterAmount(water.goalMl, water.units),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LinearProgressIndicator(
                progress = { (water.totalMl.toFloat() / water.goalMl).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                water.quickAmountsMl.forEach { ml ->
                    OutlinedButton(onClick = { onAdd(ml) }) {
                        Text(stringResource(R.string.feature_nutrition_water_add, waterAmount(ml, water.units)))
                    }
                }
                if (water.totalMl > 0) {
                    TextButton(onClick = onUndo) { Text(stringResource(R.string.feature_nutrition_water_undo)) }
                }
            }
            TextButton(onClick = { editingGoal = true }) { Text(stringResource(R.string.feature_nutrition_water_set_goal)) }
        }
    }
    if (editingGoal) {
        WaterGoalDialog(
            water = water,
            onDismiss = { editingGoal = false },
            onConfirm = { ml ->
                editingGoal = false
                onSetGoal(ml)
            },
        )
    }
}

@Composable
private fun WaterGoalDialog(water: WaterUiState, onDismiss: () -> Unit, onConfirm: (Int?) -> Unit) {
    val imperial = water.units == UnitSystem.IMPERIAL
    val initial = if (imperial) mlToFlOz(water.goalMl).roundToInt() else water.goalMl
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    val entered = text.trim().toIntOrNull()
    val goalMl = entered?.let { if (imperial) flOzToMl(it.toDouble()) else it }?.takeIf { it in WATER_GOAL_RANGE_ML }
    val range = if (imperial) {
        stringResource(
            R.string.feature_nutrition_water_range_fl_oz,
            mlToFlOz(WATER_GOAL_RANGE_ML.first).roundToInt(),
            mlToFlOz(WATER_GOAL_RANGE_ML.last).roundToInt(),
        )
    } else {
        stringResource(R.string.feature_nutrition_water_range_ml, WATER_GOAL_RANGE_ML.first, WATER_GOAL_RANGE_ML.last)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_nutrition_water_goal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(5) },
                    label = {
                        Text(
                            stringResource(
                                if (imperial) R.string.feature_nutrition_water_goal_fl_oz else R.string.feature_nutrition_water_goal_ml,
                            ),
                        )
                    },
                    supportingText = { Text(range) },
                    isError = goalMl == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    stringResource(R.string.feature_nutrition_water_goal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (water.isCustomGoal) {
                    TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.feature_nutrition_water_goal_automatic)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { goalMl?.let(onConfirm) }, enabled = goalMl != null) {
                Text(stringResource(R.string.feature_nutrition_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_nutrition_cancel)) } },
    )
}
