package com.chdjalil.jalilgym.feature.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.testing.repository.FakeFoodRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeNutritionRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryActions
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryContent
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryViewModel
import com.chdjalil.jalilgym.feature.nutrition.food.FoodDetailContent
import com.chdjalil.jalilgym.feature.nutrition.food.FoodDetailViewModel
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchActions
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchContent
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Critical UI flow 4 (docs/BLUEPRINT.md §9): search → log food → the diary totals update. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LogFoodFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val foods = FakeFoodRepository(listOf(oats, gummies))
    private val nutrition = FakeNutritionRepository(foods)
    private val prefs = FakeUserPreferencesRepository()
    private val today = LocalDate.now()

    private sealed interface Screen {
        data object Diary : Screen

        data object Search : Screen

        data class Detail(val foodId: String) : Screen
    }

    /** The three screens wired the way the nav graph wires them, without Hilt. */
    @Composable
    private fun Flow() {
        var screen by remember { mutableStateOf<Screen>(Screen.Diary) }
        val args = mapOf("date" to "$today", "meal" to "BREAKFAST")
        when (val current = screen) {
            Screen.Diary -> {
                val viewModel = remember { DiaryViewModel(nutrition) }
                val state by viewModel.uiState.collectAsState()
                DiaryContent(state, DiaryActions(onAddFood = { _, _ -> screen = Screen.Search }))
            }
            Screen.Search -> {
                val viewModel = remember { FoodSearchViewModel(SavedStateHandle(args), foods, prefs) }
                val state by viewModel.uiState.collectAsState()
                FoodSearchContent(
                    state,
                    FoodSearchActions(onOpenFood = { screen = Screen.Detail(it) }),
                    viewModel::setQuery,
                    viewModel::searchOnline,
                    viewModel::setHalalFilter,
                )
            }
            is Screen.Detail -> {
                val viewModel = remember { FoodDetailViewModel(SavedStateHandle(args + ("foodId" to current.foodId)), foods, nutrition) }
                val state by viewModel.uiState.collectAsState()
                LaunchedEffect(state.done) { if (state.done) screen = Screen.Diary }
                FoodDetailContent(
                    state,
                    onBack = {},
                    onAmount = viewModel::setAmountText,
                    onServing = viewModel::selectServing,
                    onSave = viewModel::save,
                )
            }
        }
    }

    @Test
    fun searchLogAndSeeTotals() {
        runBlocking {
            nutrition.savePlan(DietGoal.MAINTAIN, DietStyle.BALANCED, false, MacroTargets(2000, 150, 200, 70), isManualOverride = false)
        }
        composeRule.setContent { JalilgymTheme { Flow() } }

        // Empty diary: the full 2,000 kcal is left.
        composeRule.onNodeWithText("2000").assertExists()

        composeRule.onNodeWithContentDescription("Add to Breakfast").performScrollTo().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("oats")
        composeRule.onNodeWithText("Rolled oats").performClick()

        // Opens at one serving; switch to grams and log 80 g.
        composeRule.onNodeWithText("Grams").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("40")).performTextReplacement("80")
        composeRule.onNodeWithText("303 kcal").assertExists()
        composeRule.onNodeWithText("Log food").performScrollTo().performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodes(hasText("80 g · 303 kcal")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Rolled oats").assertExists()
        // 2,000 − 303 kcal left on the ring.
        composeRule.onNodeWithText("1697").assertExists()
    }
}
