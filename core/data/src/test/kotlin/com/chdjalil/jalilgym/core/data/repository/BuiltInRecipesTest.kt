package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.domain.nutrition.suggestDay
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BuiltInRecipesTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()

    @After
    fun tearDown() = database.close()

    @Test
    fun builtInRecipesImportAndPlanRealisticDays() = runTest {
        val importer = SeedImporter(context, database)
        importer.importFoods(importer.readFoodSeed())
        BuiltInRecipeImporter(database).import(importer.readRecipeSeed())
        // Importing again (a newer seed) replaces rather than duplicates.
        BuiltInRecipeImporter(database).import(importer.readRecipeSeed())

        val recipes = OfflineFirstRecipeRepository(database, database.recipeDao(), database.foodDao()).observeRecipes().first()
        assertThat(recipes.size).isAtLeast(40)
        recipes.forEach { recipe ->
            assertThat(recipe.isBuiltIn).isTrue()
            assertThat(recipe.isHalalFriendly).isTrue()
            assertThat(recipe.ingredients).isNotEmpty()
            assertThat(recipe.mealTags).isNotEmpty()
            assertThat(recipe.perServing.kcal).isGreaterThan(100.0)
        }
        MealType.entries.forEach { meal -> assertThat(recipes.count { meal.name in it.mealTags }).isAtLeast(8) }
        val recipeFoods = database.foodDao().search("").first().filter { it.food.source == FoodSource.RECIPE }
        assertThat(recipeFoods.size).isEqualTo(recipes.size)

        // Days for small to large targets all land within ±5% kcal and ±10 g protein.
        listOf(
            MacroTargets(1600, 130, 150, 50),
            MacroTargets(2000, 150, 200, 60),
            MacroTargets(2400, 180, 250, 70),
            MacroTargets(2800, 170, 330, 80),
            MacroTargets(3200, 190, 380, 90),
        ).forEach { targets ->
            (1..5).forEach { seed ->
                val plan = suggestDay(targets, recipes, Random(seed))!!
                assertThat(plan.fits(targets)).isTrue()
            }
        }
    }
}
