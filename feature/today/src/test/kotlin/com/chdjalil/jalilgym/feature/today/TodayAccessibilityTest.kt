package com.chdjalil.jalilgym.feature.today

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.TrainingStreak
import com.chdjalil.jalilgym.core.uitest.assertActionsHaveLabels
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TodayAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun todayCardsAreLabeledAndShowNutrition() {
        composeRule.setContent {
            JalilgymTheme {
                TodayContent(
                    TodayUiState(
                        isLoading = false,
                        exerciseCount = 99,
                        streak = TrainingStreak(2, 4, 1, 3, null),
                        eatenToday = Nutrients(1240.0, 95.4, 0.0, 0.0),
                        targets = MacroTargets(2200, 176, 240, 61),
                    ),
                    onOpenDay = {},
                    onChooseProgram = {},
                    onSkip = {},
                )
            }
        }
        composeRule.onNodeWithText("1240 of 2200 kcal · 95 of 176 g protein").performScrollTo()
        composeRule.assertActionsHaveLabels()
    }

    @Test
    @Config(qualifiers = "ar-ldrtl")
    fun todayReadsInArabicRightToLeft() {
        var direction: LayoutDirection? = null
        composeRule.setContent {
            direction = LocalLayoutDirection.current
            JalilgymTheme {
                TodayContent(
                    TodayUiState(isLoading = false, exerciseCount = 3, targets = MacroTargets(2200, 176, 240, 61)),
                    onOpenDay = {},
                    onChooseProgram = {},
                    onSkip = {},
                )
            }
        }
        assertThat(direction).isEqualTo(LayoutDirection.Rtl)
        composeRule.onNodeWithText("اختر برنامجًا").assertExists()
        composeRule.assertActionsHaveLabels()
    }

    @Test
    @Config(qualifiers = "fr")
    fun todayReadsInFrench() {
        composeRule.setContent {
            JalilgymTheme {
                TodayContent(TodayUiState(isLoading = false, exerciseCount = 3), onOpenDay = {}, onChooseProgram = {}, onSkip = {})
            }
        }
        composeRule.onNodeWithText("Choisir un programme").assertExists()
        composeRule.onNodeWithText("3 exercices dans votre bibliothèque").assertExists()
    }
}
