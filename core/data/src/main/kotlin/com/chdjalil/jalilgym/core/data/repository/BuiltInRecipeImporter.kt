package com.chdjalil.jalilgym.core.data.repository

import androidx.room.withTransaction
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.entity.RecipeEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeIngredientEntity
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.database.seed.RecipeSeedFile
import com.chdjalil.jalilgym.core.domain.nutrition.dietTags
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeIngredient
import javax.inject.Inject

/**
 * Loads the curated recipes (and their foods) from the seed. Seed foods must be
 * imported first. Built-in recipes are replaced in place; user recipes and
 * favorites are untouched.
 */
class BuiltInRecipeImporter @Inject constructor(private val database: JalilgymDatabase) {

    suspend fun import(seed: RecipeSeedFile, now: Long = System.currentTimeMillis()) {
        val foodDao = database.foodDao()
        val recipeDao = database.recipeDao()
        database.withTransaction {
            val recipes = seed.recipes.map { s ->
                val ingredients = s.ingredients.map { i ->
                    val food = checkNotNull(foodDao.getFood(i.food)) { "Recipe ${s.id} uses unknown food ${i.food}" }
                    RecipeIngredient(food.asExternalModel(), i.grams)
                }
                Recipe(
                    id = s.id,
                    name = s.name,
                    servings = s.servings,
                    ingredients = ingredients,
                    instructions = s.steps,
                    mealTags = s.meals,
                    isHalalFriendly = true,
                    isBuiltIn = true,
                ).let { it.copy(dietTags = dietTags(it.perServing)) }
            }
            recipeDao.replaceRecipes(
                recipes.map {
                    RecipeEntity(
                        id = it.id,
                        name = it.name,
                        servings = it.servings,
                        mealTags = it.mealTags,
                        dietTags = it.dietTags,
                        isHalal = true,
                        isBuiltIn = true,
                        instructions = it.instructions,
                        createdAt = now,
                        updatedAt = now,
                    )
                },
                recipes.flatMap { r -> r.ingredients.mapIndexed { i, ing -> RecipeIngredientEntity(r.id, i, ing.food.id, ing.grams) } },
            )
            val foods = recipes.map { recipe ->
                val foodId = "recipe-${recipe.id}"
                recipeFoodEntities(recipe, foodId, foodDao.getFood(foodId)?.food, now)
            }
            foodDao.replaceFoods(foods.map { it.first }, foods.mapNotNull { it.second })
        }
    }
}
