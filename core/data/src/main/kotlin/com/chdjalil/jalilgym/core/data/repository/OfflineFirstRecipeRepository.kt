package com.chdjalil.jalilgym.core.data.repository

import androidx.room.withTransaction
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.dao.FoodDao
import com.chdjalil.jalilgym.core.database.dao.RecipeDao
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.FoodServingEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeIngredientEntity
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.nutrition.recipeHalalStatus
import com.chdjalil.jalilgym.core.domain.repository.RecipeRepository
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.chdjalil.jalilgym.core.model.RecipeIngredient
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The food that stands in for [recipe] in search and the diary. */
internal fun recipeFoodEntities(recipe: Recipe, foodId: String, existing: FoodEntity?, now: Long): Pair<FoodEntity, FoodServingEntity?> {
    val per100 = if (recipe.totalGrams > 0) recipe.total * (100.0 / recipe.totalGrams) else recipe.total
    val food = FoodEntity(
        id = foodId,
        name = recipe.name,
        brand = null,
        barcode = null,
        source = FoodSource.RECIPE,
        offCode = null,
        kcalPer100g = per100.kcal,
        proteinPer100g = per100.proteinG,
        carbsPer100g = per100.carbsG,
        fatPer100g = per100.fatG,
        fiberPer100g = per100.fiberG,
        sugarPer100g = per100.sugarG,
        satFatPer100g = per100.satFatG,
        sodiumMgPer100g = per100.sodiumMg,
        halalStatus = recipeHalalStatus(recipe),
        isFavorite = existing?.isFavorite ?: false,
        lastUsedAt = existing?.lastUsedAt,
        cachedAt = null,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        // A recipe without ingredients can't be logged, so its food is hidden.
        deletedAt = if (recipe.totalGrams > 0) null else now,
    )
    val serving = recipe.servingGrams.takeIf { it > 0 }?.let { FoodServingEntity("$foodId-serving", foodId, "1 serving", it) }
    return food to serving
}

internal class OfflineFirstRecipeRepository @Inject constructor(
    private val database: JalilgymDatabase,
    private val recipeDao: RecipeDao,
    private val foodDao: FoodDao,
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> = recipeDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeRecipe(id: String): Flow<Recipe?> = recipeDao.observe(id).map { it?.asExternalModel() }

    override suspend fun saveRecipe(draft: RecipeDraft): String {
        val now = System.currentTimeMillis()
        val old = draft.id?.let { recipeDao.get(it)?.recipe }
        val id = old?.id ?: UUID.randomUUID().toString()
        val ingredients = draft.ingredients.filter { it.second > 0 }.mapNotNull { (foodId, grams) ->
            foodDao.getFood(foodId)?.let { RecipeIngredient(it.asExternalModel(), grams) }
        }
        val recipe = Recipe(
            id = id,
            name = draft.name.trim(),
            servings = draft.servings.coerceAtLeast(1),
            ingredients = ingredients,
            instructions = draft.instructions.map { it.trim() }.filter { it.isNotEmpty() },
            mealTags = old?.mealTags.orEmpty(),
            dietTags = old?.dietTags.orEmpty(),
            isHalalFriendly = old?.isHalal ?: false,
            isBuiltIn = old?.isBuiltIn ?: false,
        )
        val foodId = foodIdFor(id)
        database.withTransaction {
            recipeDao.replaceRecipes(
                listOf(
                    RecipeEntity(
                        id = id,
                        name = recipe.name,
                        servings = recipe.servings,
                        mealTags = recipe.mealTags,
                        dietTags = recipe.dietTags,
                        isHalal = recipe.isHalalFriendly,
                        isBuiltIn = recipe.isBuiltIn,
                        instructions = recipe.instructions,
                        createdAt = old?.createdAt ?: now,
                        updatedAt = now,
                    ),
                ),
                ingredients.mapIndexed { index, ingredient -> RecipeIngredientEntity(id, index, ingredient.food.id, ingredient.grams) },
            )
            val (food, serving) = recipeFoodEntities(recipe, foodId, foodDao.getFood(foodId)?.food, now)
            foodDao.replaceFoods(listOf(food), listOfNotNull(serving))
        }
        return id
    }

    override suspend fun deleteRecipe(id: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            recipeDao.softDelete(id, now)
            // Diary entries keep their snapshot; the food just leaves search.
            foodDao.getFood(foodIdFor(id))?.food?.let { foodDao.upsertFoods(listOf(it.copy(deletedAt = now, updatedAt = now))) }
        }
    }
}
