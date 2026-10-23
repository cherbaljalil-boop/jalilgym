package com.chdjalil.jalilgym.feature.nutrition

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
internal fun NutritionScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        JgScreenHeader(
            title = stringResource(R.string.feature_nutrition_title),
            subtitle = stringResource(R.string.feature_nutrition_subtitle),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComingSoonCard(
                title = stringResource(R.string.feature_nutrition_diary_title),
                description = stringResource(R.string.feature_nutrition_diary_desc),
                phase = 5,
                icon = JgIcons.Nutrition,
            )
            ComingSoonCard(
                title = stringResource(R.string.feature_nutrition_scan_title),
                description = stringResource(R.string.feature_nutrition_scan_desc),
                phase = 5,
                icon = JgIcons.Info,
            )
            ComingSoonCard(
                title = stringResource(R.string.feature_nutrition_targets_title),
                description = stringResource(R.string.feature_nutrition_targets_desc),
                phase = 5,
                icon = JgIcons.Progress,
            )
        }
    }
}

@Preview
@Composable
private fun NutritionPreview() {
    JalilgymTheme { NutritionScreen() }
}
