package com.chdjalil.jalilgym.feature.programs.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgramExercise
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.model.prescription
import com.chdjalil.jalilgym.feature.programs.R
import com.chdjalil.jalilgym.feature.programs.ui.SectionTitle
import com.chdjalil.jalilgym.feature.programs.ui.Stepper
import com.chdjalil.jalilgym.feature.programs.ui.descriptionRes
import com.chdjalil.jalilgym.feature.programs.ui.formatRest
import com.chdjalil.jalilgym.feature.programs.ui.labelRes

private const val REST_STEP_SECONDS = 15

/** Edits sets, rep range, rest, RPE and the progression rule of one exercise slot. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PrescriptionSheet(
    slot: ProgramExercise,
    onSave: (Prescription) -> Unit,
    onDismiss: () -> Unit,
) {
    var p by remember(slot.id) { mutableStateOf(slot.prescription) }
    val unit = when (slot.exercise.trackingType) {
        TrackingType.TIME -> stringResource(R.string.feature_programs_unit_seconds)
        TrackingType.DISTANCE -> stringResource(R.string.feature_programs_unit_meters)
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> stringResource(R.string.feature_programs_unit_reps)
    }
    val repStep = if (slot.exercise.trackingType == TrackingType.WEIGHT_REPS || slot.exercise.trackingType == TrackingType.REPS) 1 else 5
    val repLimit = if (repStep == 1) 100 else 3600

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(slot.exercise.name, style = MaterialTheme.typography.headlineSmall)

            LabeledStepper(stringResource(R.string.feature_programs_sets), p.targetSets, 1..10) {
                p = p.copy(targetSets = it)
            }
            LabeledStepper(stringResource(R.string.feature_programs_min, unit), p.repMin, 1..repLimit, repStep) {
                p = p.copy(repMin = it, repMax = maxOf(it, p.repMax))
            }
            LabeledStepper(stringResource(R.string.feature_programs_max, unit), p.repMax, 1..repLimit, repStep) {
                p = p.copy(repMax = it, repMin = minOf(it, p.repMin))
            }
            LabeledStepper(
                stringResource(R.string.feature_programs_rest),
                p.restSeconds,
                0..600,
                REST_STEP_SECONDS,
                display = ::formatRest,
            ) { p = p.copy(restSeconds = it) }

            SectionTitle(stringResource(R.string.feature_programs_target_rpe))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = p.targetRpe == null,
                    onClick = { p = p.copy(targetRpe = null) },
                    label = { Text(stringResource(R.string.feature_programs_rpe_none)) },
                )
                listOf(7.0, 7.5, 8.0, 8.5, 9.0, 9.5).forEach { rpe ->
                    FilterChip(
                        selected = p.targetRpe == rpe,
                        onClick = { p = p.copy(targetRpe = rpe) },
                        label = { Text(if (rpe % 1.0 == 0.0) "${rpe.toInt()}" else "$rpe") },
                    )
                }
            }

            SectionTitle(stringResource(R.string.feature_programs_progression))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressionRule.entries.forEach { rule ->
                    FilterChip(
                        selected = p.progressionRule == rule,
                        onClick = { p = p.copy(progressionRule = rule) },
                        label = { Text(stringResource(rule.labelRes)) },
                    )
                }
            }
            Text(
                stringResource(p.progressionRule.descriptionRes, p.incrementKg.formatKg()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (p.progressionRule != ProgressionRule.NONE) {
                LabeledStepper(
                    stringResource(R.string.feature_programs_increment),
                    (p.incrementKg * 4).toInt(),
                    1..40,
                    display = { (it / 4.0).formatKg() },
                ) { p = p.copy(incrementKg = it / 4.0) }
            }

            JgPrimaryButton(onClick = { onSave(p) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_programs_save))
            }
        }
    }
}

/** 2.5 → "2.5 kg", 5.0 → "5 kg". Weights are stored in kg; imperial display arrives with workout logging. */
private fun Double.formatKg(): String = (if (this % 1.0 == 0.0) "${toInt()}" else "$this") + " kg"

@Composable
private fun LabeledStepper(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    display: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Stepper(value = value, onValueChange = onValueChange, range = range, label = label, step = step, display = display)
    }
}
