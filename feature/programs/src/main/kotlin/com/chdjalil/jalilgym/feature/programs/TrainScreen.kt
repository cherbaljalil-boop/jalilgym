package com.chdjalil.jalilgym.feature.programs

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
import com.chdjalil.jalilgym.core.ui.EntryCard

@Composable
internal fun TrainScreen(onOpenLibrary: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_programs_title),
            subtitle = stringResource(R.string.feature_programs_subtitle),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComingSoonCard(
                title = stringResource(R.string.feature_programs_programs_title),
                description = stringResource(R.string.feature_programs_programs_desc),
                phase = 2,
                icon = JgIcons.Train,
            )
            EntryCard(
                title = stringResource(R.string.feature_programs_library_title),
                description = stringResource(R.string.feature_programs_library_desc),
                icon = JgIcons.Search,
                onClick = onOpenLibrary,
            )
            ComingSoonCard(
                title = stringResource(R.string.feature_programs_history_title),
                description = stringResource(R.string.feature_programs_history_desc),
                phase = 3,
                icon = JgIcons.Progress,
            )
        }
    }
}

@Preview
@Composable
private fun TrainPreview() {
    JalilgymTheme { TrainScreen(onOpenLibrary = {}) }
}
