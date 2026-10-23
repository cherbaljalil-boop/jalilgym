package com.chdjalil.jalilgym.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import com.chdjalil.jalilgym.core.model.ActivityLevel
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Sex
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeBodyRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeFoodRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeNutritionRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryMessage
import com.chdjalil.jalilgym.feature.nutrition.diary.DiaryViewModel
import com.chdjalil.jalilgym.feature.nutrition.food.CustomFoodViewModel
import com.chdjalil.jalilgym.feature.nutrition.food.FoodDetailViewModel
import com.chdjalil.jalilgym.feature.nutrition.navigation.mealForNow
import com.chdjalil.jalilgym.feature.nutrition.search.FoodSearchViewModel
import com.chdjalil.jalilgym.feature.nutrition.search.OnlineStatus
import com.chdjalil.jalilgym.feature.nutrition.setup.SetupViewModel
import com.chdjalil.jalilgym.feature.nutrition.setup.TargetOverride
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val foods = FakeFoodRepository(listOf(oats, gummies))
    private val nutrition = FakeNutritionRepository(foods)
    private val prefs = FakeUserPreferencesRepository()

    private fun args(vararg pairs: Pair<String, Any?>) = SavedStateHandle(mapOf("date" to "$today", "meal" to "BREAKFAST", *pairs))

    @Test
    fun `search filters by halal preference and only goes online on request`() = runTest {
        foods.online = listOf(yogurt)
        val viewModel = FoodSearchViewModel(args(), foods, prefs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.setQuery("e")
        assertThat(viewModel.uiState.value.results).isEmpty()
        viewModel.setQuery("bears")
        assertThat(viewModel.uiState.value.results.map { it.id }).containsExactly("gummies")
        assertThat(foods.searchedOnline).isEmpty()

        viewModel.setHalalFilter(HalalFilter.HIDE_FLAGGED)
        assertThat(viewModel.uiState.value.results).isEmpty()
        assertThat(viewModel.uiState.value.hiddenByFilter).isEqualTo(1)

        viewModel.setQuery("yogurt")
        viewModel.searchOnline()
        assertThat(viewModel.uiState.value.online).isEqualTo(OnlineStatus.DONE)
        assertThat(viewModel.uiState.value.results.map { it.id }).containsExactly("yogurt")

        foods.online = null
        viewModel.setQuery("granola")
        viewModel.searchOnline()
        assertThat(viewModel.uiState.value.online).isEqualTo(OnlineStatus.OFFLINE)
    }

    @Test
    fun `food detail starts at one serving and converts units`() = runTest {
        val viewModel = FoodDetailViewModel(args("foodId" to "oats"), foods, nutrition)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.amount.text).isEqualTo("1")
        assertThat(viewModel.uiState.value.grams).isEqualTo(40.0)

        viewModel.selectServing(null)
        assertThat(viewModel.uiState.value.amount.text).isEqualTo("40")
        viewModel.setAmountText("80")
        assertThat(viewModel.uiState.value.nutrients!!.kcal).isWithin(0.01).of(303.2)

        viewModel.selectMeal(MealType.LUNCH)
        viewModel.save()
        assertThat(viewModel.uiState.value.done).isTrue()
        val entry = nutrition.entries.value.single()
        assertThat(entry.meal).isEqualTo(MealType.LUNCH)
        assertThat(entry.quantityG).isEqualTo(80.0)
    }

    @Test
    fun `editing an entry starts from its amount and updates it`() = runTest {
        val id = nutrition.logFood(today, MealType.BREAKFAST, "oats", 60.0)
        val viewModel = FoodDetailViewModel(args("foodId" to "oats", "entryId" to id), foods, nutrition)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.isEditing).isTrue()
        assertThat(viewModel.uiState.value.amount.text).isEqualTo("60")
        viewModel.setAmountText("90")
        viewModel.save()
        assertThat(nutrition.entries.value.single().quantityG).isEqualTo(90.0)
    }

    @Test
    fun `setup computes targets, applies overrides and saves everything`() = runTest {
        val body = FakeBodyRepository()
        val viewModel = SetupViewModel(prefs, body, nutrition)

        viewModel.setSex(Sex.MALE)
        viewModel.setBirthDate(today.minusYears(30))
        viewModel.setHeight("180")
        assertThat(viewModel.uiState.value.computed).isNull()
        viewModel.setWeight("80")
        viewModel.setActivity(ActivityLevel.MODERATE)
        viewModel.setGoal(DietGoal.CUT)
        viewModel.setStyle(DietStyle.HIGH_PROTEIN)
        viewModel.setHalalFilter(HalalFilter.CERTIFIED_ONLY)

        val computed = viewModel.uiState.value.computed!!
        assertThat(computed.tdee).isEqualTo(2759)
        assertThat(computed.targets.proteinG).isEqualTo(192)

        viewModel.setOverride(TargetOverride(protein = "200"))
        assertThat(viewModel.uiState.value.finalTargets!!.proteinG).isEqualTo(200)
        assertThat(viewModel.uiState.value.finalTargets!!.calories).isEqualTo(computed.targets.calories)

        viewModel.save()
        assertThat(viewModel.uiState.value.saved).isTrue()
        val plan = nutrition.plan.value!!
        assertThat(plan.goal).isEqualTo(DietGoal.CUT)
        assertThat(plan.halalOnly).isTrue()
        assertThat(plan.isManualOverride).isTrue()
        assertThat(plan.targets.proteinG).isEqualTo(200)
        assertThat(prefs.current.bodyProfile!!.heightCm).isEqualTo(180.0)
        assertThat(prefs.current.halalFilter).isEqualTo(HalalFilter.CERTIFIED_ONLY)
        assertThat(body.weights.value.single().weightKg).isEqualTo(80.0)
    }

    @Test
    fun `setup reads imperial units and prefills from earlier answers`() = runTest {
        val imperial = FakeUserPreferencesRepository(UserPreferences(units = UnitSystem.IMPERIAL))
        val body = FakeBodyRepository().apply { logWeight(today, 81.0) }
        val viewModel = SetupViewModel(imperial, body, nutrition)

        assertThat(viewModel.uiState.value.weight).isEqualTo("178.6")
        viewModel.setSex(Sex.FEMALE)
        viewModel.setBirthDate(today.minusYears(40))
        viewModel.setHeight("65")
        viewModel.save()
        assertThat(imperial.current.bodyProfile!!.heightCm).isWithin(0.01).of(165.1)
        // Same weight as the log: not logged twice.
        assertThat(body.weights.value).hasSize(1)
    }

    @Test
    fun `diary copies the previous day's meal and reports what happened`() = runTest {
        nutrition.logFood(today.minusDays(1), MealType.BREAKFAST, "oats", 50.0)
        val viewModel = DiaryViewModel(nutrition)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val messages = mutableListOf<DiaryMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect { messages += it } }

        viewModel.copyFromPreviousDay(MealType.BREAKFAST)
        viewModel.copyFromPreviousDay(MealType.DINNER)
        viewModel.quickAdd(MealType.SNACK, "Bar", Nutrients(200.0, 20.0, 20.0, 5.0))

        assertThat(messages).containsExactly(DiaryMessage.Copied(1), DiaryMessage.NothingToCopy).inOrder()
        val state = viewModel.uiState.value
        assertThat(state.isToday).isTrue()
        assertThat(state.day.entries).hasSize(2)
        assertThat(state.day.totals.kcal).isWithin(0.01).of(379 * 0.5 + 200)
        assertThat(state.loggingStreak).isEqualTo(2)

        viewModel.previousDay()
        assertThat(viewModel.uiState.value.isToday).isFalse()
        assertThat(viewModel.uiState.value.day.entries).hasSize(1)
    }

    @Test
    fun `custom food converts per-serving values to per 100 g`() = runTest {
        val viewModel = CustomFoodViewModel(SavedStateHandle(mapOf("barcode" to "123456")), foods)
        viewModel.update { copy(name = "Protein bar", basisGrams = "40", servingLabel = "1 bar", kcal = "200", protein = "20") }
        viewModel.save()

        val id = viewModel.uiState.value.savedFoodId!!
        val bar = foods.observeFood(id).first()!!
        assertThat(bar.barcode).isEqualTo("123456")
        assertThat(bar.per100g.kcal).isEqualTo(500.0)
        assertThat(bar.per100g.proteinG).isEqualTo(50.0)
        assertThat(bar.servings.single().grams).isEqualTo(40.0)
    }

    @Test
    fun `meal for now follows the time of day`() {
        assertThat(mealForNow(LocalTime.of(7, 30))).isEqualTo(MealType.BREAKFAST)
        assertThat(mealForNow(LocalTime.of(13, 0))).isEqualTo(MealType.LUNCH)
        assertThat(mealForNow(LocalTime.of(19, 0))).isEqualTo(MealType.DINNER)
        assertThat(mealForNow(LocalTime.of(23, 0))).isEqualTo(MealType.SNACK)
    }
}
