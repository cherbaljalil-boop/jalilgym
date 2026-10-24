package com.chdjalil.jalilgym.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncError
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncStatus
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

    @Test
    fun cloudSyncSignedInAndEnteringACode() {
        var status by mutableStateOf(CloudSyncStatus(available = true, email = "a@b.co", lastSyncedAt = 1L, pendingChanges = 3))
        composeRule.setContent {
            JalilgymTheme {
                ProfileContent(ProfileUiState.Ready(UserPreferences()), versionName = "1.0.0", onEvent = {}, cloudStatus = status)
            }
        }
        composeRule.onNodeWithText("Signed in as a@b.co").performScrollTo()
        composeRule.onNodeWithText("3 changes waiting to upload").assertExists()
        composeRule.assertActionsHaveLabels()

        status = CloudSyncStatus(available = true, codeSentTo = "a@b.co", error = CloudSyncError.INVALID_CODE)
        composeRule.onNodeWithText("That code is wrong or has expired. Try again or send a new one.").performScrollTo()
        composeRule.assertActionsHaveLabels()
    }
}
