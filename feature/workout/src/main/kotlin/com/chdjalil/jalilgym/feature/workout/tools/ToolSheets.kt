package com.chdjalil.jalilgym.feature.workout.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.domain.units.barOptions
import com.chdjalil.jalilgym.core.domain.units.defaultBar
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.domain.units.standardPlates
import com.chdjalil.jalilgym.core.domain.units.toKg
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.domain.workout.ONE_REP_MAX_RELIABLE_REPS
import com.chdjalil.jalilgym.core.domain.workout.OneRepMaxFormula
import com.chdjalil.jalilgym.core.domain.workout.calculatePlates
import com.chdjalil.jalilgym.core.domain.workout.estimateOneRepMax
import com.chdjalil.jalilgym.core.domain.workout.generateWarmups
import com.chdjalil.jalilgym.core.domain.workout.percentTable
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.workout.R

/** All tools work in the user's display unit; kg is only used at the boundary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun String.toNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlateCalculatorSheet(units: UnitSystem, initialKg: Double?, onDismiss: () -> Unit) {
    var target by rememberSaveable { mutableStateOf(initialKg?.let { formatNumber(units.fromKg(it)) }.orEmpty()) }
    var bar by rememberSaveable { mutableDoubleStateOf(units.defaultBar) }
    val unit = units.weightSymbol
    ToolSheet(stringResource(R.string.feature_workout_plates_title), onDismiss) {
        NumberField(stringResource(R.string.feature_workout_target_weight, unit), target, { target = it }, Modifier.fillMaxWidth())
        Text(stringResource(R.string.feature_workout_bar), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            units.barOptions.forEach { option ->
                FilterChip(selected = bar == option, onClick = { bar = option }, label = { Text("${formatNumber(option)} $unit") })
            }
        }
        val goal = target.toNumber()
        if (goal != null) {
            val load = calculatePlates(goal, bar, units.standardPlates)
            Text(stringResource(R.string.feature_workout_per_side), style = MaterialTheme.typography.titleSmall)
            if (load.perSide.isEmpty()) {
                Text(stringResource(R.string.feature_workout_just_the_bar), style = MaterialTheme.typography.bodyLarge)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    load.perSide.forEach { plate -> AssistChip(onClick = {}, label = { Text(formatNumber(plate)) }) }
                }
            }
            Text(
                if (load.shortfall(goal) > 0.01) {
                    stringResource(R.string.feature_workout_closest_load, formatNumber(load.achieved), unit)
                } else {
                    stringResource(R.string.feature_workout_exact_load, formatNumber(load.achieved), unit)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun OneRepMaxSheet(units: UnitSystem, initialKg: Double?, initialReps: Int?, onDismiss: () -> Unit) {
    var weight by rememberSaveable { mutableStateOf(initialKg?.let { formatNumber(units.fromKg(it)) }.orEmpty()) }
    var reps by rememberSaveable { mutableStateOf(initialReps?.toString().orEmpty()) }
    val unit = units.weightSymbol
    ToolSheet(stringResource(R.string.feature_workout_one_rm_title), onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(stringResource(R.string.feature_workout_weight_unit, unit), weight, { weight = it }, Modifier.weight(1f))
            NumberField(stringResource(R.string.feature_workout_reps), reps, { reps = it }, Modifier.weight(1f))
        }
        val w = weight.toNumber()
        val r = reps.toNumber()?.toInt()
        val epley = if (w != null && r != null) estimateOneRepMax(w, r, OneRepMaxFormula.EPLEY) else null
        val brzycki = if (w != null && r != null) estimateOneRepMax(w, r, OneRepMaxFormula.BRZYCKI) else null
        if (epley != null) {
            Text("${formatNumber(epley)} $unit", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(
                    R.string.feature_workout_one_rm_formulas,
                    "${formatNumber(epley)} $unit",
                    brzycki?.let { "${formatNumber(it)} $unit" } ?: "—",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if ((r ?: 0) > ONE_REP_MAX_RELIABLE_REPS) {
                Text(
                    stringResource(R.string.feature_workout_one_rm_unreliable, ONE_REP_MAX_RELIABLE_REPS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            percentTable(epley).forEach { (percent, value) ->
                Row {
                    Text("$percent%", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${formatNumber(value)} $unit", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Suggests warm-ups for [workingKg] and inserts them as (kg, reps) via [onInsert]. */
@Composable
internal fun WarmupSheet(units: UnitSystem, workingKg: Double?, onInsert: (List<Pair<Double, Int>>) -> Unit, onDismiss: () -> Unit) {
    var working by rememberSaveable { mutableStateOf(workingKg?.let { formatNumber(units.fromKg(it)) }.orEmpty()) }
    val unit = units.weightSymbol
    ToolSheet(stringResource(R.string.feature_workout_warmup_title), onDismiss) {
        NumberField(stringResource(R.string.feature_workout_working_weight, unit), working, { working = it }, Modifier.fillMaxWidth())
        val w = working.toNumber()
        val warmups = if (w != null) generateWarmups(w, units.defaultBar, units.standardPlates) else emptyList()
        if (w != null && warmups.isEmpty()) {
            Text(stringResource(R.string.feature_workout_warmup_none), style = MaterialTheme.typography.bodyMedium)
        }
        warmups.forEach { set ->
            Row {
                Text("${formatNumber(set.weight)} $unit", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.feature_workout_times_reps, set.reps), style = MaterialTheme.typography.titleMedium)
            }
        }
        JgPrimaryButton(
            onClick = { onInsert(warmups.map { units.toKg(it.weight) to it.reps }) },
            enabled = warmups.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.feature_workout_add_warmups)) }
    }
}
