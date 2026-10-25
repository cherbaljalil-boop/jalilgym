package com.chdjalil.jalilgym.feature.programs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.ProgramExercise
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.ui.ExerciseImage
import com.chdjalil.jalilgym.feature.programs.R

/** "3 × 8–12 · 2:00 rest" (seconds or meters instead of reps for timed/distance work). */
@Composable
internal fun prescriptionSummary(slot: ProgramExercise): String {
    val reps = if (slot.repMin == slot.repMax) "${slot.repMin}" else "${slot.repMin}–${slot.repMax}"
    val target = when (slot.exercise.trackingType) {
        TrackingType.TIME -> stringResource(R.string.feature_programs_target_seconds, slot.targetSets, reps)
        TrackingType.DISTANCE -> stringResource(R.string.feature_programs_target_meters, slot.targetSets, reps)
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> stringResource(R.string.feature_programs_target_reps, slot.targetSets, reps)
    }
    return stringResource(R.string.feature_programs_target_with_rest, target, formatRest(slot.restSeconds))
}

internal fun formatRest(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

internal val ProgressionRule.labelRes: Int
    get() = when (this) {
        ProgressionRule.DOUBLE -> R.string.feature_programs_rule_double
        ProgressionRule.LINEAR -> R.string.feature_programs_rule_linear
        ProgressionRule.NONE -> R.string.feature_programs_rule_none
    }

internal val ProgressionRule.descriptionRes: Int
    get() = when (this) {
        ProgressionRule.DOUBLE -> R.string.feature_programs_rule_double_desc
        ProgressionRule.LINEAR -> R.string.feature_programs_rule_linear_desc
        ProgressionRule.NONE -> R.string.feature_programs_rule_none_desc
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_programs_back))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

/** Name prompt used for new programs, new days and renames. */
@Composable
internal fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_programs_cancel)) } },
    )
}

/**
 * One exercise in a day. A lime bar on the left marks superset members, so
 * consecutive rows of the same superset read as one block.
 */
@Composable
internal fun ExerciseSlotRow(
    slot: ProgramExercise,
    inSuperset: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val barColor = MaterialTheme.colorScheme.primary
    Box(
        // Drawn over the list item, whose background would otherwise cover it.
        modifier.drawWithContent {
            drawContent()
            if (inSuperset) drawRect(barColor, size = Size(4.dp.toPx(), size.height))
        },
    ) {
        ListItem(
            headlineContent = { Text(slot.exercise.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(prescriptionSummary(slot), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingContent = {
                ExerciseImage(
                    path = slot.exercise.images.firstOrNull(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
}
