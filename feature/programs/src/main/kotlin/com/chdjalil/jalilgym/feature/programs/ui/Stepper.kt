package com.chdjalil.jalilgym.feature.programs.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.feature.programs.R

/** − value + control with big touch targets; [display] formats the value. */
@Composable
internal fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier,
    step: Int = 1,
    display: (Int) -> String = { it.toString() },
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
        ) { Icon(painterResource(JgIcons.Remove), stringResource(R.string.feature_programs_decrease, label)) }
        Text(
            display(value),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 56.dp)
                .semantics { stateDescription = "$label ${display(value)}" },
        )
        FilledTonalIconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
        ) { Icon(painterResource(JgIcons.Add), stringResource(R.string.feature_programs_increase, label)) }
    }
}
