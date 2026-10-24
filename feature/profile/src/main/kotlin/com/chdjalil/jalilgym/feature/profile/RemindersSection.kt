package com.chdjalil.jalilgym.feature.profile

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.model.ReminderSettings
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
internal fun RemindersSection(settings: ReminderSettings, onChange: (ReminderSettings) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    SectionTitle(R.string.feature_profile_reminders)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_profile_reminders_workout), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.feature_profile_reminders_workout_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in settings.workoutDays
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onChange(settings.copy(workoutDays = if (selected) settings.workoutDays - day else settings.workoutDays + day))
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    )
                }
            }
            if (settings.workoutDays.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.feature_profile_reminders_time), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { pickingTime = true }) {
                        Text(settings.workoutTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)))
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.weeklySummary,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(weeklySummary = it)) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_profile_reminders_summary), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.feature_profile_reminders_summary_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.weeklySummary, onCheckedChange = null)
            }
        }
    }
    if (pickingTime) {
        ReminderTimeDialog(
            initial = settings.workoutTime,
            onDismiss = { pickingTime = false },
            onConfirm = {
                pickingTime = false
                onChange(settings.copy(workoutTime = it))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = DateFormat.is24HourFormat(LocalContext.current))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_profile_reminders_time)) },
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.feature_profile_reminders_time_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_profile_reminders_time_cancel)) } },
    )
}
