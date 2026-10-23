package com.chdjalil.jalilgym.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chdjalil.jalilgym.core.designsystem.component.JgScreenHeader
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.ui.ComingSoonCard

@Composable
internal fun ProgressScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_progress_title),
            subtitle = stringResource(R.string.feature_progress_subtitle),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComingSoonCard(
                title = stringResource(R.string.feature_progress_strength_title),
                description = stringResource(R.string.feature_progress_strength_desc),
                phase = 4,
                icon = JgIcons.Progress,
            )
            ComingSoonCard(
                title = stringResource(R.string.feature_progress_records_title),
                description = stringResource(R.string.feature_progress_records_desc),
                phase = 4,
                icon = JgIcons.Train,
            )
            ComingSoonCard(
                title = stringResource(R.string.feature_progress_body_title),
                description = stringResource(R.string.feature_progress_body_desc),
                phase = 4,
                icon = JgIcons.Profile,
            )
        }
    }
}

@Preview
@Composable
private fun ProgressPreview() {
    JalilgymTheme { ProgressScreen() }
}
