package com.chdjalil.jalilgym.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeIngredientEntity
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeIngredient

data class PopulatedRecipeIngredient(
    @Embedded val ingredient: RecipeIngredientEntity,
    @Relation(entity = FoodEntity::class, parentColumn = "foodId", entityColumn = "id")
    val food: PopulatedFood,
)

data class PopulatedRecipe(
    @Embedded val recipe: RecipeEntity,
    @Relation(entity = RecipeIngredientEntity::class, parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<PopulatedRecipeIngredient>,
)

fun PopulatedRecipe.asExternalModel() = Recipe(
    id = recipe.id,
    name = recipe.name,
    servings = recipe.servings,
    ingredients = ingredients.sortedBy { it.ingredient.position }
        .map { RecipeIngredient(it.food.asExternalModel(), it.ingredient.grams) },
    instructions = recipe.instructions,
    mealTags = recipe.mealTags,
    dietTags = recipe.dietTags,
    isHalalFriendly = recipe.isHalal,
    isBuiltIn = recipe.isBuiltIn,
)
