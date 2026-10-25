package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.DiaryDay
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.SavedMeal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** The diet plan, food diary and saved meals (docs/BLUEPRINT.md §3.7). */
interface NutritionRepository {
    fun observeActivePlan(): Flow<DietPlan?>

    /** Replaces the active plan. */
    suspend fun savePlan(goal: DietGoal, style: DietStyle, halalOnly: Boolean, targets: MacroTargets, isManualOverride: Boolean)

    fun observeDiary(date: LocalDate): Flow<DiaryDay>

    /** Logs [grams] of a food; its nutrients are snapshotted. Returns the entry id. */
    suspend fun logFood(date: LocalDate, meal: MealType, foodId: String, grams: Double): String

    /** Logs calories and macros without a food. */
    suspend fun quickAdd(date: LocalDate, meal: MealType, name: String, nutrients: Nutrients): String

    /** Changes the amount of a food entry, scaling its snapshot. */
    suspend fun updateEntryQuantity(entryId: String, grams: Double)

    suspend fun deleteEntry(entryId: String)

    /** Copies one meal's entries to another day (e.g. "copy yesterday's breakfast"). Returns how many. */
    suspend fun copyMeal(from: LocalDate, to: LocalDate, meal: MealType): Int

    fun observeSavedMeals(): Flow<List<SavedMeal>>

    /** Saves the foods logged in a meal as a reusable meal. Null when there's nothing to save. */
    suspend fun saveMealFromDiary(name: String, date: LocalDate, meal: MealType): String?

    suspend fun logSavedMeal(savedMealId: String, date: LocalDate, meal: MealType)

    suspend fun deleteSavedMeal(savedMealId: String)

    /** Days in a row with something logged, ending today (or yesterday). */
    fun observeLoggingStreak(): Flow<Int>
}
