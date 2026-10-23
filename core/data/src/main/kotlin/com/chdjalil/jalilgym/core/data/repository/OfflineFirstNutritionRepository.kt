package com.chdjalil.jalilgym.core.data.repository

import androidx.room.withTransaction
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.dao.DietPlanDao
import com.chdjalil.jalilgym.core.database.dao.FoodDao
import com.chdjalil.jalilgym.core.database.dao.FoodLogDao
import com.chdjalil.jalilgym.core.database.dao.SavedMealDao
import com.chdjalil.jalilgym.core.database.entity.DietPlanEntity
import com.chdjalil.jalilgym.core.database.entity.FoodLogEntryEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealItemEntity
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.nutrition.currentLoggingStreak
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.model.DiaryDay
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.SavedMeal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

internal class OfflineFirstNutritionRepository @Inject constructor(
    private val database: JalilgymDatabase,
    private val foodDao: FoodDao,
    private val foodLogDao: FoodLogDao,
    private val savedMealDao: SavedMealDao,
    private val dietPlanDao: DietPlanDao,
) : NutritionRepository {

    private fun now() = System.currentTimeMillis()

    private fun newId() = UUID.randomUUID().toString()

    override fun observeActivePlan(): Flow<DietPlan?> = dietPlanDao.observeActive().map { it?.asExternalModel() }

    override suspend fun savePlan(
        goal: DietGoal,
        style: DietStyle,
        halalOnly: Boolean,
        targets: MacroTargets,
        isManualOverride: Boolean,
    ) {
        val now = now()
        dietPlanDao.activate(
            DietPlanEntity(
                id = newId(),
                goal = goal,
                style = style,
                halalOnly = halalOnly,
                calories = targets.calories,
                proteinG = targets.proteinG,
                carbsG = targets.carbsG,
                fatG = targets.fatG,
                isManualOverride = isManualOverride,
                startDate = LocalDate.now(),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override fun observeDiary(date: LocalDate): Flow<DiaryDay> =
        foodLogDao.observeDay(date).map { entries -> DiaryDay(date, entries.map { it.asExternalModel() }) }

    private fun entryFor(food: Food, date: LocalDate, meal: MealType, grams: Double, now: Long): FoodLogEntryEntity {
        val n = food.nutrientsFor(grams)
        return FoodLogEntryEntity(
            id = newId(),
            date = date,
            meal = meal,
            foodId = food.id,
            recipeId = null,
            quantityG = grams,
            nameSnapshot = food.brand?.let { "${food.name} · $it" } ?: food.name,
            kcal = n.kcal,
            proteinG = n.proteinG,
            carbsG = n.carbsG,
            fatG = n.fatG,
            fiberG = n.fiberG,
            sugarG = n.sugarG,
            sodiumMg = n.sodiumMg,
            loggedAt = now,
            updatedAt = now,
            satFatG = n.satFatG,
        )
    }

    override suspend fun logFood(date: LocalDate, meal: MealType, foodId: String, grams: Double): String {
        val food = checkNotNull(foodDao.getFood(foodId)) { "No food $foodId" }.asExternalModel()
        val now = now()
        val entry = entryFor(food, date, meal, grams, now)
        database.withTransaction {
            foodLogDao.upsertEntries(listOf(entry))
            foodDao.markUsed(foodId, now)
        }
        return entry.id
    }

    override suspend fun quickAdd(date: LocalDate, meal: MealType, name: String, nutrients: Nutrients): String {
        val now = now()
        val entry = FoodLogEntryEntity(
            id = newId(),
            date = date,
            meal = meal,
            foodId = null,
            recipeId = null,
            quantityG = 0.0,
            nameSnapshot = name.trim(),
            kcal = nutrients.kcal,
            proteinG = nutrients.proteinG,
            carbsG = nutrients.carbsG,
            fatG = nutrients.fatG,
            fiberG = nutrients.fiberG,
            sugarG = nutrients.sugarG,
            sodiumMg = nutrients.sodiumMg,
            loggedAt = now,
            updatedAt = now,
            satFatG = nutrients.satFatG,
        )
        foodLogDao.upsertEntries(listOf(entry))
        return entry.id
    }

    override suspend fun updateEntryQuantity(entryId: String, grams: Double) {
        val entry = foodLogDao.getEntry(entryId) ?: return
        // Quick-add entries have no amount to scale.
        if (entry.quantityG <= 0 || grams <= 0) return
        val f = grams / entry.quantityG
        foodLogDao.upsertEntries(
            listOf(
                entry.copy(
                    quantityG = grams,
                    kcal = entry.kcal * f,
                    proteinG = entry.proteinG * f,
                    carbsG = entry.carbsG * f,
                    fatG = entry.fatG * f,
                    fiberG = entry.fiberG?.times(f),
                    sugarG = entry.sugarG?.times(f),
                    sodiumMg = entry.sodiumMg?.times(f),
                    satFatG = entry.satFatG?.times(f),
                    updatedAt = now(),
                ),
            ),
        )
    }

    override suspend fun deleteEntry(entryId: String) = foodLogDao.deleteEntry(entryId)

    override suspend fun copyMeal(from: LocalDate, to: LocalDate, meal: MealType): Int {
        val now = now()
        val copies = foodLogDao.entriesFor(from, meal).mapIndexed { index, entry ->
            // Keeps the original snapshot; offsets keep the order stable.
            entry.copy(id = newId(), date = to, loggedAt = now + index, updatedAt = now)
        }
        foodLogDao.upsertEntries(copies)
        return copies.size
    }

    override fun observeSavedMeals(): Flow<List<SavedMeal>> =
        savedMealDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override suspend fun saveMealFromDiary(name: String, date: LocalDate, meal: MealType): String? {
        val entries = foodLogDao.entriesFor(date, meal).filter { it.foodId != null && it.quantityG > 0 }
        if (entries.isEmpty()) return null
        val now = now()
        val saved = SavedMealEntity(newId(), name.trim(), meal, createdAt = now, updatedAt = now)
        savedMealDao.insertMealWithItems(
            saved,
            entries.mapIndexed { index, entry -> SavedMealItemEntity(saved.id, index, entry.foodId!!, entry.quantityG) },
        )
        return saved.id
    }

    override suspend fun logSavedMeal(savedMealId: String, date: LocalDate, meal: MealType) {
        val saved = savedMealDao.get(savedMealId)?.asExternalModel() ?: return
        val now = now()
        val entries = saved.items.mapIndexed { index, item -> entryFor(item.food, date, meal, item.quantityG, now + index) }
        database.withTransaction {
            foodLogDao.upsertEntries(entries)
            saved.items.forEach { foodDao.markUsed(it.food.id, now) }
        }
    }

    override suspend fun deleteSavedMeal(savedMealId: String) = savedMealDao.delete(savedMealId)

    override fun observeLoggingStreak(): Flow<Int> =
        foodLogDao.observeDailyTotals().map { rows -> currentLoggingStreak(rows.map { it.date }, LocalDate.now()) }
}
