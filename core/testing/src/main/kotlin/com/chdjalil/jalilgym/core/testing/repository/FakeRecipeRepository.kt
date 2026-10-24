package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.RecipeRepository
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodServing
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.chdjalil.jalilgym.core.model.RecipeIngredient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Resolves ingredients from, and mirrors recipes as foods into, [foods]. */
class FakeRecipeRepository(private val foods: FakeFoodRepository = FakeFoodRepository()) : RecipeRepository {
    val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    private var nextId = 1

    override fun observeRecipes(): Flow<List<Recipe>> = recipes

    override fun observeRecipe(id: String): Flow<Recipe?> = recipes.map { list -> list.find { it.id == id } }

    override suspend fun saveRecipe(draft: RecipeDraft): String {
        val id = draft.id ?: "recipe-${nextId++}"
        val recipe = Recipe(
            id = id,
            name = draft.name.trim(),
            servings = draft.servings,
            ingredients = draft.ingredients.mapNotNull { (foodId, grams) ->
                foods.foods.value.find { it.id == foodId }?.let { RecipeIngredient(it, grams) }
            },
            instructions = draft.instructions,
        )
        recipes.update { list -> list.filterNot { it.id == id } + recipe }
        val food = Food(
            id = foodIdFor(id),
            name = recipe.name,
            brand = null,
            barcode = null,
            source = FoodSource.RECIPE,
            per100g = recipe.total * (100.0 / recipe.totalGrams.coerceAtLeast(1.0)),
            halalStatus = HalalStatus.UNKNOWN,
            servings = listOf(FoodServing("1 serving", recipe.servingGrams)),
        )
        foods.foods.update { list -> list.filterNot { it.id == food.id } + food }
        return id
    }

    override suspend fun deleteRecipe(id: String) {
        recipes.update { list -> list.filterNot { it.id == id } }
        foods.foods.update { list -> list.filterNot { it.id == foodIdFor(id) } }
    }
}
