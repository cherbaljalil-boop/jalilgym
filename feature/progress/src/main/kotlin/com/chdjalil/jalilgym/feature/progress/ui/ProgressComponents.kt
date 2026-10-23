package com.chdjalil.jalilgym.feature.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.domain.units.weightSymbol
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.progress.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(JgIcons.ArrowBack), stringResource(R.string.feature_progress_back))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            content()
        }
    }
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

internal fun formatDate(date: LocalDate): String = dateFormat.format(date)

internal fun formatDate(millis: Long): String = formatDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())

internal fun UnitSystem.formatWeight(kg: Double): String = "${formatNumber(fromKg(kg))} $weightSymbol"

/** Signed change, e.g. "+1.5 kg" or "−0.8 kg". */
internal fun UnitSystem.formatWeightChange(kg: Double): String = "${formatSigned(fromKg(kg))} $weightSymbol"

/** "+1.5", "−0.8" or "0". */
internal fun formatSigned(value: Double): String {
    val sign = if (value >= 0.05) "+" else if (value <= -0.05) "−" else ""
    return sign + formatNumber(kotlin.math.abs(value))
}

private const val CM_PER_INCH = 2.54

/** Measurements are stored in cm (body fat in %). */
internal fun UnitSystem.fromCm(type: MeasurementType, value: Double): Double =
    if (type == MeasurementType.BODY_FAT || this == UnitSystem.METRIC) value else value / CM_PER_INCH

internal fun UnitSystem.toCm(type: MeasurementType, value: Double): Double =
    if (type == MeasurementType.BODY_FAT || this == UnitSystem.METRIC) value else value * CM_PER_INCH

internal fun UnitSystem.measurementSymbol(type: MeasurementType): String = when {
    type == MeasurementType.BODY_FAT -> "%"
    this == UnitSystem.METRIC -> "cm"
    else -> "in"
}

internal fun UnitSystem.formatMeasurement(type: MeasurementType, value: Double): String =
    "${formatNumber(fromCm(type, value))} ${measurementSymbol(type)}"

/** Number entry dialog; [onConfirm] gets the parsed value in the shown unit. */
@Composable
internal fun NumberInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    initial: String = "",
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val value = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                extraContent()
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) {
                Text(stringResource(R.string.feature_progress_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_progress_cancel)) } },
    )
}
