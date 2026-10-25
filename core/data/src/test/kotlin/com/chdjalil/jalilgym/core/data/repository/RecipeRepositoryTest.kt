package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RecipeRepositoryTest {

    private lateinit var database: JalilgymDatabase
    private lateinit var recipes: OfflineFirstRecipeRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()
        recipes = OfflineFirstRecipeRepository(database, database.recipeDao(), database.foodDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun recipesBecomeLoggableFoodsAndStayInSync() = runTest {
        val importer = SeedImporter(ApplicationProvider.getApplicationContext(), database)
        importer.importFoods(importer.readFoodSeed())
        val foods = OfflineFirstFoodRepository(database.foodDao(), FakeOffline)

        val id = recipes.saveRecipe(
            RecipeDraft(
                name = "Overnight oats",
                servings = 2,
                ingredients = listOf("seed-oats" to 80.0, "seed-milk-semi-skimmed" to 300.0, "seed-banana" to 0.0),
                instructions = listOf("Mix", " ", "Chill overnight"),
            ),
        )

        val recipe = recipes.observeRecipe(id).first()!!
        assertThat(recipe.ingredients).hasSize(2)
        assertThat(recipe.instructions).containsExactly("Mix", "Chill overnight").inOrder()
        assertThat(recipe.perServing.kcal).isWithin(0.01).of((379 * 0.8 + 50 * 3.0) / 2)
        assertThat(recipe.servingGrams).isEqualTo(190.0)

        val food = foods.observeFood(recipes.foodIdFor(id)).first()!!
        assertThat(food.source).isEqualTo(FoodSource.RECIPE)
        assertThat(food.halalStatus).isEqualTo(HalalStatus.UNKNOWN)
        assertThat(food.servings.single().grams).isEqualTo(190.0)
        assertThat(food.nutrientsFor(190.0).kcal).isWithin(0.01).of(recipe.perServing.kcal)
        assertThat(foods.searchLocal("overnight").first().map { it.id }).containsExactly(food.id)

        // Logging a serving works like any food.
        val nutrition = OfflineFirstNutritionRepository(database, database.foodDao(), database.foodLogDao(), database.savedMealDao(), database.dietPlanDao())
        nutrition.logFood(LocalDate.now(), MealType.BREAKFAST, food.id, 190.0)

        // Editing updates the food; deleting hides it but keeps the diary.
        recipes.saveRecipe(RecipeDraft(id = id, name = "Overnight oats", servings = 1, ingredients = listOf("seed-oats" to 80.0)))
        assertThat(foods.observeFood(food.id).first()!!.servings.single().grams).isEqualTo(80.0)
        recipes.deleteRecipe(id)
        assertThat(recipes.observeRecipes().first()).isEmpty()
        assertThat(foods.searchLocal("overnight").first()).isEmpty()
        assertThat(nutrition.observeDiary(LocalDate.now()).first().entries.single().name).isEqualTo("Overnight oats")
    }

    private object FakeOffline : com.chdjalil.jalilgym.core.network.OpenFoodFactsDataSource {
        override suspend fun search(query: String, pageSize: Int) = throw java.io.IOException("offline")

        override suspend fun product(barcode: String) = throw java.io.IOException("offline")
    }
}
