package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.domain.repository.BarcodeLookup
import com.chdjalil.jalilgym.core.domain.repository.OnlineSearch
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.FoodDraft
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.network.OffProduct
import com.chdjalil.jalilgym.core.network.OpenFoodFactsDataSource
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Open Food Facts stand-in; set [offline] to simulate no connection. */
private class FakeOpenFoodFacts : OpenFoodFactsDataSource {
    var offline = false
    val products = mutableMapOf<String, OffProduct>()
    var calls = 0

    override suspend fun search(query: String, pageSize: Int): List<OffProduct> {
        calls++
        if (offline) throw IOException("offline")
        return products.values.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun product(barcode: String): OffProduct? {
        calls++
        if (offline) throw IOException("offline")
        return products[barcode]
    }
}

private fun product(code: String, name: String, kcal: Double?, labels: List<String> = emptyList(), ingredients: String? = null) =
    OffProduct(
        code = code,
        name = name,
        brand = "Acme",
        kcal = kcal,
        proteinG = 10.0,
        carbsG = 20.0,
        fatG = 5.0,
        fiberG = null,
        sugarG = 3.0,
        satFatG = null,
        sodiumMg = 400.0,
        labelTags = labels,
        ingredientsText = ingredients,
        servingLabel = "1 pot (150 g)",
        servingGrams = 150.0,
    )

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NutritionRepositoriesTest {

    private lateinit var database: JalilgymDatabase
    private val off = FakeOpenFoodFacts()
    private lateinit var foods: OfflineFirstFoodRepository
    private lateinit var nutrition: OfflineFirstNutritionRepository
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()
        foods = OfflineFirstFoodRepository(database.foodDao(), off)
        nutrition = OfflineFirstNutritionRepository(
            database,
            database.foodDao(),
            database.foodLogDao(),
            database.savedMealDao(),
            database.dietPlanDao(),
        )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seed() {
        val importer = SeedImporter(ApplicationProvider.getApplicationContext(), database)
        importer.importFoods(importer.readFoodSeed())
    }

    @Test
    fun bundledFoodsAreSearchableOffline() = runTest {
        seed()
        off.offline = true
        val results = foods.searchLocal("chicken").first()
        assertThat(results.map { it.name }).contains("Chicken breast, cooked")
        assertThat(results.first().servings).isNotEmpty()
        assertThat(foods.searchOnline("chicken")).isEqualTo(OnlineSearch.Offline)
    }

    @Test
    fun onlineResultsAreCachedForOfflineUseAndKeepUserChoices() = runTest {
        off.products["111"] = product("111", "Greek yogurt", 97.0, labels = listOf("en:halal"))
        off.products["222"] = product("222", "Gummy bears", 340.0, ingredients = "sugar, gelatine")
        off.products["333"] = product("333", "Mystery", kcal = null)

        val online = foods.searchOnline("e") as OnlineSearch.Results
        // Products without energy can't be logged, so they're skipped.
        assertThat(online.foods.map { it.name }).containsExactly("Greek yogurt", "Gummy bears")
        val bears = online.foods.single { it.name == "Gummy bears" }
        assertThat(bears.halalStatus).isEqualTo(HalalStatus.FLAGGED)
        assertThat(online.foods.single { it.name == "Greek yogurt" }.halalStatus).isEqualTo(HalalStatus.CERTIFIED)

        val yogurt = online.foods.single { it.name == "Greek yogurt" }
        foods.setFavorite(yogurt.id, true)
        off.offline = true
        assertThat(foods.searchLocal("yogurt").first().single().isFavorite).isTrue()
        assertThat(foods.lookupBarcode("111")).isInstanceOf(BarcodeLookup.Found::class.java)

        // Re-caching keeps the favorite flag.
        off.offline = false
        foods.searchOnline("yogurt")
        assertThat(foods.observeFood(yogurt.id).first()!!.isFavorite).isTrue()
    }

    @Test
    fun barcodeLookupUsesCacheThenNetwork() = runTest {
        off.products["555"] = product("555", "Protein bar", 380.0)
        val found = foods.lookupBarcode("555") as BarcodeLookup.Found
        assertThat(found.food.servings.single().grams).isEqualTo(150.0)
        val calls = off.calls
        assertThat(foods.lookupBarcode("555")).isInstanceOf(BarcodeLookup.Found::class.java)
        assertThat(off.calls).isEqualTo(calls)

        assertThat(foods.lookupBarcode("999")).isEqualTo(BarcodeLookup.NotFound("999"))
        off.offline = true
        assertThat(foods.lookupBarcode("888")).isEqualTo(BarcodeLookup.Offline("888"))
    }

    @Test
    fun customFoodsCanCarryABarcodeAndHalalMark() = runTest {
        val id = foods.saveCustomFood(
            FoodDraft(name = " Mum's lentil soup ", barcode = "777", per100g = Nutrients(80.0, 5.0, 10.0, 2.0), markedHalal = true),
        )
        val food = (foods.lookupBarcode("777") as BarcodeLookup.Found).food
        assertThat(food.id).isEqualTo(id)
        assertThat(food.name).isEqualTo("Mum's lentil soup")
        assertThat(food.halalStatus).isEqualTo(HalalStatus.USER_MARKED)
        foods.setUserMarkedHalal(id, false)
        assertThat(foods.observeFood(id).first()!!.halalStatus).isEqualTo(HalalStatus.UNKNOWN)
    }

    @Test
    fun diaryTotalsSnapshotsCopiesAndSavedMeals() = runTest {
        seed()
        nutrition.logFood(today, MealType.BREAKFAST, "seed-oats", 80.0)
        val eggs = nutrition.logFood(today, MealType.BREAKFAST, "seed-egg-whole", 100.0)
        nutrition.quickAdd(today, MealType.SNACK, "Coffee shop muffin", Nutrients(420.0, 6.0, 55.0, 19.0))

        var day = nutrition.observeDiary(today).first()
        assertThat(day.entriesFor(MealType.BREAKFAST)).hasSize(2)
        assertThat(day.totals.kcal).isWithin(0.01).of(379 * 0.8 + 143 + 420)

        nutrition.updateEntryQuantity(eggs, 150.0)
        day = nutrition.observeDiary(today).first()
        assertThat(day.entries.single { it.id == eggs }.nutrients.proteinG).isWithin(0.01).of(12.6 * 1.5)

        // Used foods show up as recents.
        assertThat(foods.observeRecent().first().map { it.id }).containsAtLeast("seed-oats", "seed-egg-whole")

        assertThat(nutrition.copyMeal(today, today.plusDays(1), MealType.BREAKFAST)).isEqualTo(2)
        assertThat(nutrition.observeDiary(today.plusDays(1)).first().totals.kcal)
            .isWithin(0.01).of(379 * 0.8 + 143 * 1.5)

        val savedId = nutrition.saveMealFromDiary("My breakfast", today, MealType.BREAKFAST)!!
        assertThat(nutrition.saveMealFromDiary("Nothing", today, MealType.DINNER)).isNull()
        val saved = nutrition.observeSavedMeals().first().single()
        assertThat(saved.items.map { it.quantityG }).containsExactly(80.0, 150.0).inOrder()
        nutrition.logSavedMeal(savedId, today.minusDays(1), MealType.LUNCH)
        assertThat(nutrition.observeDiary(today.minusDays(1)).first().entriesFor(MealType.LUNCH)).hasSize(2)

        // Yesterday, today and tomorrow are logged; the streak counts up to today.
        assertThat(nutrition.observeLoggingStreak().first()).isEqualTo(2)

        nutrition.deleteEntry(eggs)
        assertThat(nutrition.observeDiary(today).first().entries).hasSize(2)
    }

    @Test
    fun savingAPlanReplacesTheActiveOne() = runTest {
        nutrition.savePlan(DietGoal.CUT, DietStyle.BALANCED, false, MacroTargets(2200, 176, 240, 61), isManualOverride = false)
        nutrition.savePlan(DietGoal.LEAN_BULK, DietStyle.HIGH_PROTEIN, true, MacroTargets(3000, 190, 380, 83), isManualOverride = true)
        val plan = nutrition.observeActivePlan().first()!!
        assertThat(plan.goal).isEqualTo(DietGoal.LEAN_BULK)
        assertThat(plan.halalOnly).isTrue()
        assertThat(plan.targets.calories).isEqualTo(3000)
    }

    @Test
    fun loggingFoodUnlocksNutritionBadges() = runTest {
        seed()
        val achievements = OfflineFirstAchievementRepository(
            database.progressDao(),
            database.bodyDao(),
            database.programDao(),
            database.foodLogDao(),
            database.dietPlanDao(),
        )
        nutrition.savePlan(DietGoal.MAINTAIN, DietStyle.BALANCED, false, MacroTargets(2000, 150, 200, 70), isManualOverride = true)
        (0L until 7).forEach { daysAgo ->
            nutrition.quickAdd(today.minusDays(daysAgo), MealType.LUNCH, "Day $daysAgo", Nutrients(1950.0, 150.0, 200.0, 70.0))
        }

        assertThat(achievements.syncUnlocks()).containsAtLeast("first_meal", "logging_7", "macros_7")
        val status = achievements.observeAchievements().first().associateBy { it.id }
        assertThat(status.getValue("logging_30").progress).isEqualTo(7.0)
    }
}
