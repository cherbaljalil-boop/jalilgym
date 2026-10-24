package com.chdjalil.jalilgym.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeFoodRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeRecipeRepository
import com.chdjalil.jalilgym.feature.nutrition.recipes.RecipeEditorViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val foods = FakeFoodRepository(listOf(oats, yogurt))
    private val recipes = FakeRecipeRepository(foods)

    @Test
    fun buildsARecipeFromSearchedIngredients() = runTest {
        val viewModel = RecipeEditorViewModel(SavedStateHandle(), recipes, foods)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.setName("Yogurt bowl")
        viewModel.setIngredientQuery("oat")
        assertThat(viewModel.uiState.value.ingredientResults.map { it.id }).containsExactly("oats")
        viewModel.addIngredient(oats)
        // Added at one serving; the search clears.
        assertThat(viewModel.uiState.value.ingredients.single().grams).isEqualTo("40")
        assertThat(viewModel.uiState.value.ingredientQuery).isEmpty()
        viewModel.addIngredient(yogurt)
        viewModel.setIngredientGrams(1, "200")
        viewModel.setServings(2)

        val state = viewModel.uiState.value
        assertThat(state.servingGrams).isEqualTo(120.0)
        assertThat(state.perServing.kcal).isWithin(0.01).of((379 * 0.4 + 97 * 2.0) / 2)

        viewModel.save()
        assertThat(viewModel.uiState.value.saved).isTrue()
        val saved = recipes.recipes.value.single()
        assertThat(saved.ingredients.map { it.grams }).containsExactly(40.0, 200.0).inOrder()
        assertThat(foods.observeFood(recipes.foodIdFor(saved.id)).first()).isNotNull()
    }

    @Test
    fun editingLoadsTheRecipeAndBuiltInsAreCopied() = runTest {
        val id = recipes.saveRecipe(RecipeDraft(name = "Oats", servings = 1, ingredients = listOf("oats" to 50.0)))
        recipes.recipes.value = recipes.recipes.value.map { it.copy(isBuiltIn = true) }
        val viewModel = RecipeEditorViewModel(SavedStateHandle(mapOf("recipeId" to id)), recipes, foods)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.name).isEqualTo("Oats")
        assertThat(viewModel.uiState.value.isBuiltIn).isTrue()
        viewModel.setName("My oats")
        viewModel.save()

        assertThat(recipes.recipes.value.map { it.name }).containsExactly("Oats", "My oats")
    }
}
