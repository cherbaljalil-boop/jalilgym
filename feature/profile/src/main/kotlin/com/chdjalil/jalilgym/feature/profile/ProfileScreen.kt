package com.chdjalil.jalilgym.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgCreatedByCredit
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.component.JgWordmark
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences

@Composable
internal fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    ProfileContent(uiState = uiState, versionName = versionName, onEvent = viewModel::onEvent)
}

@Composable
internal fun ProfileContent(
    uiState: ProfileUiState,
    versionName: String,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(title = stringResource(R.string.feature_profile_title))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState is ProfileUiState.Ready) {
                SettingsSection(uiState.preferences, onEvent)
            }
            AboutSection(versionName)
        }
    }
}

@Composable
private fun SettingsSection(preferences: UserPreferences, onEvent: (ProfileEvent) -> Unit) {
    SectionTitle(R.string.feature_profile_settings)
    ChoiceSetting(
        label = R.string.feature_profile_theme,
        options = listOf(
            ThemeMode.DARK to R.string.feature_profile_theme_dark,
            ThemeMode.LIGHT to R.string.feature_profile_theme_light,
            ThemeMode.SYSTEM to R.string.feature_profile_theme_system,
        ),
        selected = preferences.themeMode,
        onSelect = { onEvent(ProfileEvent.ThemeSelected(it)) },
    )
    ChoiceSetting(
        label = R.string.feature_profile_units,
        options = listOf(
            UnitSystem.METRIC to R.string.feature_profile_units_metric,
            UnitSystem.IMPERIAL to R.string.feature_profile_units_imperial,
        ),
        selected = preferences.units,
        onSelect = { onEvent(ProfileEvent.UnitsSelected(it)) },
    )
    ChoiceSetting(
        label = R.string.feature_profile_effort,
        options = listOf(
            EffortScale.RPE to R.string.feature_profile_effort_rpe,
            EffortScale.RIR to R.string.feature_profile_effort_rir,
            EffortScale.OFF to R.string.feature_profile_effort_off,
        ),
        selected = preferences.effortScale,
        onSelect = { onEvent(ProfileEvent.EffortScaleSelected(it)) },
    )
}

@Composable
private fun <T> ChoiceSetting(
    @StringRes label: Int,
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(text))
                }
            }
        }
    }
}

@Composable
private fun AboutSection(versionName: String) {
    SectionTitle(R.string.feature_profile_about)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JgWordmark(style = MaterialTheme.typography.displaySmall)
            JgCreatedByCredit()
            if (versionName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.feature_profile_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.feature_profile_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.feature_profile_attributions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview
@Composable
private fun ProfilePreview() {
    JalilgymTheme {
        ProfileContent(ProfileUiState.Ready(UserPreferences()), versionName = "0.1.0", onEvent = {})
    }
}
