package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.nutrition.currentLoggingStreak
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.model.DiaryDay
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.FoodLogEntry
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.SavedMeal
import com.chdjalil.jalilgym.core.model.SavedMealItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Logs foods from [foods] (shared with a [FakeFoodRepository]). */
class FakeNutritionRepository(private val foods: FakeFoodRepository = FakeFoodRepository()) : NutritionRepository {
    val plan = MutableStateFlow<DietPlan?>(null)
    val entries = MutableStateFlow<List<FoodLogEntry>>(emptyList())
    val savedMeals = MutableStateFlow<List<SavedMeal>>(emptyList())
    private var nextId = 1

    private fun id(prefix: String) = "$prefix-${nextId++}"

    override fun observeActivePlan(): Flow<DietPlan?> = plan

    override suspend fun savePlan(goal: DietGoal, style: DietStyle, halalOnly: Boolean, targets: MacroTargets, isManualOverride: Boolean) {
        plan.value = DietPlan(id("plan"), goal, style, halalOnly, targets, isManualOverride, LocalDate.now())
    }

    override fun observeDiary(date: LocalDate): Flow<DiaryDay> = entries.map { list -> DiaryDay(date, list.filter { it.date == date }) }

    override suspend fun logFood(date: LocalDate, meal: MealType, foodId: String, grams: Double): String {
        val food = checkNotNull(foods.foods.value.find { it.id == foodId })
        val entry = FoodLogEntry(id("entry"), date, meal, foodId, food.name, grams, food.nutrientsFor(grams))
        entries.update { it + entry }
        foods.markUsed(foodId)
        return entry.id
    }

    override suspend fun quickAdd(date: LocalDate, meal: MealType, name: String, nutrients: Nutrients): String {
        val entry = FoodLogEntry(id("entry"), date, meal, null, name, 0.0, nutrients)
        entries.update { it + entry }
        return entry.id
    }

    override suspend fun updateEntryQuantity(entryId: String, grams: Double) = entries.update { list ->
        list.map { e -> if (e.id == entryId && e.quantityG > 0) e.copy(quantityG = grams, nutrients = e.nutrients * (grams / e.quantityG)) else e }
    }

    override suspend fun deleteEntry(entryId: String) = entries.update { list -> list.filterNot { it.id == entryId } }

    override suspend fun copyMeal(from: LocalDate, to: LocalDate, meal: MealType): Int {
        val copies = entries.value.filter { it.date == from && it.meal == meal }.map { it.copy(id = id("entry"), date = to) }
        entries.update { it + copies }
        return copies.size
    }

    override fun observeSavedMeals(): Flow<List<SavedMeal>> = savedMeals

    override suspend fun saveMealFromDiary(name: String, date: LocalDate, meal: MealType): String? {
        val items = entries.value.filter { it.date == date && it.meal == meal && it.foodId != null }.mapNotNull { e ->
            foods.foods.value.find { it.id == e.foodId }?.let { SavedMealItem(it, e.quantityG) }
        }
        if (items.isEmpty()) return null
        val saved = SavedMeal(id("meal"), name, meal, items)
        savedMeals.update { it + saved }
        return saved.id
    }

    override suspend fun logSavedMeal(savedMealId: String, date: LocalDate, meal: MealType) {
        val saved = savedMeals.value.find { it.id == savedMealId } ?: return
        saved.items.forEach { logFood(date, meal, it.food.id, it.quantityG) }
    }

    override suspend fun deleteSavedMeal(savedMealId: String) = savedMeals.update { list -> list.filterNot { it.id == savedMealId } }

    override fun observeLoggingStreak(): Flow<Int> = entries.map { list -> currentLoggingStreak(list.map { it.date }, LocalDate.now()) }
}
