package com.chdjalil.jalilgym.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsAuthorCredit() {
        setContent()

        composeRule.onNodeWithText("Created by CH.Djalil").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.2.3").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectingLightThemeSendsEvent() {
        val events = mutableListOf<ProfileEvent>()
        setContent(onEvent = { events += it })

        composeRule.onNodeWithText("Dark").assertIsSelected()
        composeRule.onNodeWithText("Light").performClick()

        assertThat(events).containsExactly(ProfileEvent.ThemeSelected(ThemeMode.LIGHT))
    }

    private fun setContent(onEvent: (ProfileEvent) -> Unit = {}) {
        composeRule.setContent {
            JalilgymTheme {
                ProfileContent(
                    uiState = ProfileUiState.Ready(UserPreferences()),
                    versionName = "1.2.3",
                    onEvent = onEvent,
                )
            }
        }
    }
}
