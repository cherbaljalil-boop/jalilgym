package com.chdjalil.jalilgym.feature.nutrition

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.designsystem.theme.JalilgymTheme
import com.chdjalil.jalilgym.core.domain.nutrition.DayPlan
import com.chdjalil.jalilgym.core.domain.nutrition.SuggestedMeal
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.chdjalil.jalilgym.core.testing.repository.FakeFoodRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeRecipeRepository
import com.chdjalil.jalilgym.core.uitest.assertActionsHaveLabels
import com.chdjalil.jalilgym.feature.nutrition.ideas.MealIdeasContent
import com.chdjalil.jalilgym.feature.nutrition.ideas.MealIdeasUiState
import com.chdjalil.jalilgym.feature.nutrition.recipes.EditableIngredient
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipeEditorContent
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipeEditorUiState
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipesContent
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipesUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RecipeAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val foods = FakeFoodRepository(listOf(oats, yogurt))
    private val recipes = FakeRecipeRepository(foods)

    @Test
    fun recipeScreensAreLabeled() {
        val recipe = runBlocking {
            recipes.observeRecipe(recipes.saveRecipe(RecipeDraft(name = "Oats", servings = 1, ingredients = listOf("oats" to 50.0)))).first()!!
        }
        composeRule.setContent {
            JalilgymTheme { RecipesContent(RecipesUiState(isLoading = false, mine = listOf(recipe)), onBack = {}, onOpenRecipe = {}) }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun editorIsLabeled() {
        composeRule.setContent {
            JalilgymTheme {
                RecipeEditorContent(
                    RecipeEditorUiState(
                        isLoading = false,
                        name = "Bowl",
                        ingredients = listOf(EditableIngredient(oats, "40")),
                        ingredientResults = listOf(yogurt),
                        isNew = false,
                        foodId = "recipe-1",
                    ),
                    onBack = {},
                )
            }
        }
        composeRule.assertActionsHaveLabels()
    }

    @Test
    fun mealIdeasAreLabeled() {
        val recipe = runBlocking {
            recipes.observeRecipe(recipes.saveRecipe(RecipeDraft(name = "Oats", servings = 1, ingredients = listOf("oats" to 100.0)))).first()!!
        }
        val plan = DayPlan(MealType.entries.map { SuggestedMeal(it, recipe, 1.0) })
        composeRule.setContent {
            JalilgymTheme {
                MealIdeasContent(MealIdeasUiState(isLoading = false, targets = MacroTargets(2000, 150, 200, 70), plan = plan, logged = setOf(1)), onBack = {})
            }
        }
        composeRule.assertActionsHaveLabels()
    }
}
