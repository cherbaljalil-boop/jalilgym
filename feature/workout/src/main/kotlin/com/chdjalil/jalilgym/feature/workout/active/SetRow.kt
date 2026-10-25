package com.chdjalil.jalilgym.feature.workout.active

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JgTheme
import com.chdjalil.jalilgym.core.domain.units.formatWeightValue
import com.chdjalil.jalilgym.core.domain.units.parseWeightToKg
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.feature.workout.R

internal val TYPE_COLUMN = 40.dp
internal val PREVIOUS_COLUMN = 72.dp
internal val EFFORT_COLUMN = 52.dp
internal val CHECK_COLUMN = 48.dp

/**
 * One set: type badge · previous · weight · reps · effort · done.
 * Text fields keep their own text while typing ("82." stays "82.") and save
 * every valid value straight away, so nothing is lost if the app is killed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SetRow(
    set: WorkoutSet,
    workingNumber: Int,
    previous: WorkoutSet?,
    units: UnitSystem,
    effortScale: EffortScale,
    onEdit: ((WorkoutSet) -> WorkoutSet) -> Unit,
    onCycleType: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val extended = JgTheme.extendedColors
    val rowColor = if (set.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var menuOpen by remember { mutableStateOf(false) }
        val (badge, badgeColor) = when (set.type) {
            SetType.WARMUP -> stringResource(R.string.feature_workout_set_warmup_short) to extended.setWarmup
            SetType.DROP -> stringResource(R.string.feature_workout_set_drop_short) to extended.setDrop
            SetType.FAILURE -> stringResource(R.string.feature_workout_set_failure_short) to extended.setFailure
            SetType.WORKING -> workingNumber.toString() to MaterialTheme.colorScheme.onSurface
        }
        val typeDescription = stringResource(R.string.feature_workout_set_type_description, setTypeLabel(set.type))
        Box(
            Modifier
                .width(TYPE_COLUMN)
                .combinedClickable(onClick = onCycleType, onLongClick = { menuOpen = true })
                .semantics { contentDescription = typeDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, style = MaterialTheme.typography.titleMedium, color = badgeColor)
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_workout_delete_set)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
        Text(
            previous?.let { prev ->
                val w = prev.weightKg?.let { units.formatWeightValue(it) }
                if (w != null && prev.reps != null) "$w×${prev.reps}" else prev.reps?.toString() ?: "—"
            } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(PREVIOUS_COLUMN),
            maxLines = 1,
        )
        var weightText by rememberSaveable(set.id) { mutableStateOf(set.weightKg?.let { units.formatWeightValue(it) }.orEmpty()) }
        CompactNumberField(
            value = weightText,
            onValueChange = { text ->
                weightText = text
                if (text.isBlank()) {
                    onEdit { it.copy(weightKg = null) }
                } else {
                    units.parseWeightToKg(text)?.let { kg -> onEdit { it.copy(weightKg = kg) } }
                }
            },
            decimal = true,
            description = stringResource(R.string.feature_workout_weight_field, workingNumber),
            modifier = Modifier
                .weight(1f)
                .testTag("weight_${set.id}"),
        )
        var repsText by rememberSaveable(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
        CompactNumberField(
            value = repsText,
            onValueChange = { text ->
                val digits = text.filter(Char::isDigit).take(4)
                repsText = digits
                onEdit { it.copy(reps = digits.toIntOrNull()) }
            },
            decimal = false,
            description = stringResource(R.string.feature_workout_reps_field, workingNumber),
            modifier = Modifier
                .weight(1f)
                .testTag("reps_${set.id}"),
        )
        if (effortScale != EffortScale.OFF) {
            EffortCell(set, effortScale, onEdit)
        }
        FilledIconToggleButton(
            checked = set.completed,
            onCheckedChange = { onToggleComplete() },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                checkedContainerColor = MaterialTheme.colorScheme.primary,
                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .size(CHECK_COLUMN)
                .testTag("complete_${set.id}"),
        ) {
            Icon(
                painterResource(JgIcons.Check),
                contentDescription = stringResource(
                    if (set.completed) R.string.feature_workout_mark_not_done else R.string.feature_workout_mark_done,
                ),
            )
        }
    }
}

@Composable
private fun setTypeLabel(type: SetType): String = stringResource(
    when (type) {
        SetType.WARMUP -> R.string.feature_workout_set_warmup
        SetType.WORKING -> R.string.feature_workout_set_working
        SetType.DROP -> R.string.feature_workout_set_drop
        SetType.FAILURE -> R.string.feature_workout_set_failure
    },
)

@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    decimal: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.semantics { contentDescription = description },
    )
}

private val RPE_VALUES = listOf(6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
private val RIR_VALUES = (0..5).toList()

@Composable
private fun EffortCell(set: WorkoutSet, scale: EffortScale, onEdit: ((WorkoutSet) -> WorkoutSet) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = when (scale) {
        EffortScale.RPE -> set.rpe?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" }
        EffortScale.RIR -> set.rir?.toString()
        EffortScale.OFF -> null
    }
    Box(Modifier.width(EFFORT_COLUMN), contentAlignment = Alignment.Center) {
        TextButton(onClick = { open = true }) {
            Text(
                label ?: stringResource(if (scale == EffortScale.RPE) R.string.feature_workout_rpe else R.string.feature_workout_rir),
                color = if (label == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_workout_effort_clear)) },
                onClick = {
                    open = false
                    onEdit { it.copy(rpe = null, rir = null) }
                },
            )
            if (scale == EffortScale.RPE) {
                RPE_VALUES.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(if (value % 1.0 == 0.0) "${value.toInt()}" else "$value") },
                        onClick = {
                            open = false
                            onEdit { it.copy(rpe = value) }
                        },
                    )
                }
            } else {
                RIR_VALUES.forEach { value ->
                    DropdownMenuItem(
                        text = { Text("$value") },
                        onClick = {
                            open = false
                            onEdit { it.copy(rir = value) }
                        },
                    )
                }
            }
        }
    }
}

/** Small round marker used for "superset" in exercise headers. */
@Composable
internal fun Dot(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}
