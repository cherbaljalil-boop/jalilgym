package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeDraft
import kotlinx.coroutines.flow.Flow

/**
 * The user's recipes and the built-in ones (docs/BLUEPRINT.md §3.7). Every
 * recipe has a matching food (see [foodIdFor]) with "1 serving" as its serving,
 * so it can be searched and logged like any food.
 */
interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>

    fun observeRecipe(id: String): Flow<Recipe?>

    /** Creates or updates a recipe and its food; returns the recipe id. */
    suspend fun saveRecipe(draft: RecipeDraft): String

    /** Removes a user recipe; past diary entries keep their snapshot. */
    suspend fun deleteRecipe(id: String)

    fun foodIdFor(recipeId: String): String = "recipe-$recipeId"
}
