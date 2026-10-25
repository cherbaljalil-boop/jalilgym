package com.chdjalil.jalilgym.feature.nutrition

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.nutrition.computeTargets
import com.chdjalil.jalilgym.core.model.DiaryDay
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.FoodLogEntry
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.uitest.assertActionsHaveLabels
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryActions
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryContent
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryUiState
import com.chdjalil.jalilgym.feature.nutrition.food.FoodDetailContent
import com.chdjalil.jalilgym.feature.nutrition.food.FoodDetailUiState
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchActions
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchContent
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchUiState
import com.chdjalil.jalilgym.feature.nutrition.setup.SetupContent
import com.chdjalil.jalilgym.feature.nutrition.setup.SetupUiState
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Every button, chip and row on the nutrition screens has something for TalkBack to read. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NutritionAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.now()

    @Test
    fun diary() {
        val entry = FoodLogEntry("e1", today, MealType.BREAKFAST, "oats", "Rolled oats", 80.0, oats.nutrientsFor(80.0))
        val plan = DietPlan("p", DietGoal.MAINTAIN, DietStyle.BALANCED, false, MacroTargets(2000, 150, 200, 70), false, today)
        composeRule.setContent {
            JalilgymTheme {
                DiaryContent(DiaryUiState(isLoading = false, day = DiaryDay(today, listOf(entry)), plan = plan, loggingStreak = 3), DiaryActions())
            }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun search() {
        composeRule.setContent {
            JalilgymTheme {
                FoodSearchContent(
                    FoodSearchUiState(today, MealType.LUNCH, query = "", favorites = listOf(yogurt), recents = listOf(oats, gummies)),
                    FoodSearchActions(),
                    {},
                    {},
                    {},
                )
            }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun foodDetail() {
        composeRule.setContent {
            JalilgymTheme { FoodDetailContent(FoodDetailUiState(isLoading = false, food = oats), onBack = {}) }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun setup() {
        val computed = computeTargets(DietGoal.CUT, DietStyle.BALANCED, 2759.0, 80.0, Sex.MALE)
        composeRule.setContent {
            JalilgymTheme {
                SetupContent(SetupUiState(isLoading = false, sex = Sex.MALE, birthDate = today.minusYears(30), computed = computed), onBack = {})
            }
        }
        composeRule.assertActionsHaveLabels()
    }
}
