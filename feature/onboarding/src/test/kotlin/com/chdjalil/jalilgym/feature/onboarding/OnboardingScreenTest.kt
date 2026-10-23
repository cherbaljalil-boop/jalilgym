package com.chdjalil.jalilgym.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun beginnerIsGuidedToFullBody() {
        val preferences = FakeUserPreferencesRepository()
        val viewModel = OnboardingViewModel(preferences)
        var finished = false
        composeRule.setContent {
            JalilgymTheme { OnboardingScreen(onFinished = { finished = true }, viewModel = viewModel) }
        }

        composeRule.onNodeWithText("Created by CH.Djalil").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").performClick()

        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
        composeRule.onNodeWithText("Build muscle").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.onNodeWithText("Beginner").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.onNodeWithText("Suggested for you".uppercase()).assertIsDisplayed()
        composeRule.onNodeWithText("Full Body · 3 days a week").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Start training").performClick()

        composeRule.waitForIdle()
        assertThat(finished).isTrue()
        assertThat(preferences.current.starterProgram).isEqualTo(StarterProgram.FULL_BODY_3)
    }
}
