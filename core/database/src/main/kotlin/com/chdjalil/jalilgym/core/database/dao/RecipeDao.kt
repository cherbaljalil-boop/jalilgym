package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.RecipeEntity
import com.chdjalil.jalilgym.core.database.entity.RecipeIngredientEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    /** The user's recipes first, then built-in ones, by name. */
    @Transaction
    @Query("SELECT * FROM recipes WHERE deletedAt IS NULL ORDER BY isBuiltIn, name")
    fun observeAll(): Flow<List<PopulatedRecipe>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id AND deletedAt IS NULL")
    fun observe(id: String): Flow<PopulatedRecipe?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: String): PopulatedRecipe?

    @Upsert
    suspend fun upsertRecipes(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId IN (:recipeIds)")
    suspend fun deleteIngredientsFor(recipeIds: List<String>)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Transaction
    suspend fun replaceRecipes(recipes: List<RecipeEntity>, ingredients: List<RecipeIngredientEntity>) {
        upsertRecipes(recipes)
        deleteIngredientsFor(recipes.map { it.id })
        insertIngredients(ingredients)
    }

    @Query("UPDATE recipes SET deletedAt = :now, updatedAt = :now WHERE id = :id AND isBuiltIn = 0")
    suspend fun softDelete(id: String, now: Long)
}
