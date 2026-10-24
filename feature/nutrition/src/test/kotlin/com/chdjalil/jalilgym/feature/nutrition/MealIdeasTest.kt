package com.chdjalil.jalilgym.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeFoodRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeGroceryRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeNutritionRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeRecipeRepository
import com.chdjalil.jalilgym.feature.nutrition.ideas.MealIdeasViewModel
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MealIdeasTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val foods = FakeFoodRepository(listOf(oats, yogurt, gummies))
    private val nutrition = FakeNutritionRepository(foods)
    private val recipes = FakeRecipeRepository(foods)

    /** Built-in-style recipes, mirrored as foods so they can be logged. */
    private suspend fun seedRecipes() {
        listOf(
            Triple("Oats", oats, listOf(MealType.BREAKFAST, MealType.SNACK)),
            Triple("Yogurt", yogurt, listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)),
        ).forEach { (name, food, meals) ->
            val id = recipes.saveRecipe(RecipeDraft(name = name, servings = 1, ingredients = listOf(food.id to 300.0)))
            recipes.recipes.value = recipes.recipes.value.map { if (it.id == id) it.copy(mealTags = meals.map(MealType::name), isBuiltIn = true) else it }
        }
    }

    @Test
    fun suggestsSwapsAndLogsADay() = runTest {
        seedRecipes()
        nutrition.savePlan(DietGoal.MAINTAIN, DietStyle.BALANCED, false, MacroTargets(2000, 120, 200, 70), isManualOverride = false)
        val viewModel = MealIdeasViewModel(SavedStateHandle(mapOf("seed" to 3L)), nutrition, recipes, FakeGroceryRepository())

        val plan = viewModel.uiState.value.plan!!
        assertThat(plan.meals.map { it.meal }).containsExactly(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).inOrder()

        viewModel.log(0)
        viewModel.log(0)
        assertThat(nutrition.entries.value).hasSize(1)
        assertThat(nutrition.entries.value.single().meal).isEqualTo(MealType.BREAKFAST)
        assertThat(nutrition.entries.value.single().quantityG).isEqualTo(plan.meals[0].grams)

        viewModel.logAll()
        assertThat(nutrition.entries.value).hasSize(4)
        assertThat(nutrition.entries.value.map { it.date }.distinct()).containsExactly(LocalDate.now())
        assertThat(viewModel.uiState.value.logged).containsExactly(0, 1, 2, 3)

        // Swapping un-logs that meal so it can be logged again.
        viewModel.swap(0)
        assertThat(viewModel.uiState.value.logged).doesNotContain(0)
    }

    @Test
    fun needsTargetsAndSameSeedSameDay() = runTest {
        seedRecipes()
        val viewModel = MealIdeasViewModel(SavedStateHandle(mapOf("seed" to 9L)), nutrition, recipes, FakeGroceryRepository())
        assertThat(viewModel.uiState.value.targets).isNull()
        assertThat(viewModel.uiState.value.plan).isNull()

        nutrition.savePlan(DietGoal.CUT, DietStyle.BALANCED, false, MacroTargets(1800, 130, 170, 60), isManualOverride = false)
        val first = viewModel.uiState.value.plan!!
        val again = MealIdeasViewModel(SavedStateHandle(mapOf("seed" to 9L)), nutrition, recipes, FakeGroceryRepository()).uiState.value.plan
        assertThat(again).isEqualTo(first)
    }
}
