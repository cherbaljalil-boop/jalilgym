package com.chdjalil.jalilgym.feature.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.uitest.assertActionsHaveLabels
import com.chdjalil.jalilgym.feature.profile.licenses.LicensesScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ProfileAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profile() {
        composeRule.setContent {
            JalilgymTheme { ProfileContent(ProfileUiState.Ready(UserPreferences()), versionName = "1.0.0", onEvent = {}) }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun licenses() {
        composeRule.setContent { JalilgymTheme { LicensesScreen(onBack = {}) } }
        composeRule.assertActionsHaveLabels()
    }
}
