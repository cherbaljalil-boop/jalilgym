package com.chdjalil.jalilgym.core.designsystem.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.chdjalil.jalilgym.core.designsystem.R

/** "JALILGYM" wordmark with the lime accent on "GYM". */
@Composable
fun JgWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
) {
    val appName = stringResource(R.string.core_designsystem_app_name)
    val jalil = stringResource(R.string.core_designsystem_wordmark_jalil)
    val gym = stringResource(R.string.core_designsystem_wordmark_gym)
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = LocalContentColor.current)) { append(jalil) }
            withStyle(SpanStyle(color = accent)) { append(gym) }
        },
        style = style,
        modifier = modifier.semantics { contentDescription = appName },
    )
}

/** The author credit, shown on the welcome screen, About and the splash screen. */
@Composable
fun JgCreatedByCredit(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.core_designsystem_created_by),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
