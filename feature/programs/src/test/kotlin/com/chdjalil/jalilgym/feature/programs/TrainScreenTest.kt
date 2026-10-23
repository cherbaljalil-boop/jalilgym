package com.chdjalil.jalilgym.feature.programs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.feature.programs.train.TrainContent
import com.chdjalil.jalilgym.feature.programs.train.TrainUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TrainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withoutProgramOffersTemplates() {
        var templatesOpened = 0
        composeRule.setContent {
            JalilgymTheme {
                TrainContent(TrainUiState(isLoading = false), {}, onOpenTemplates = { templatesOpened++ }, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("No active program").performScrollTo()
        composeRule.onNodeWithText("Browse templates").performClick()

        assertThat(templatesOpened).isEqualTo(1)
    }

    @Test
    fun activeProgramShowsNextDay() {
        val opened = mutableListOf<String>()
        val program = pplTemplate.copy(id = "mine", isTemplate = false)
        composeRule.setContent {
            JalilgymTheme {
                TrainContent(
                    TrainUiState(isLoading = false, active = ActiveProgram(program, nextDayIndex = 1, startedAt = 0L), programs = listOf(program)),
                    {}, {}, {}, onOpenDay = { opened += it }, {},
                )
            }
        }

        composeRule.onNodeWithText("View Pull").performClick()

        assertThat(opened).containsExactly("t-pull")
    }
}
