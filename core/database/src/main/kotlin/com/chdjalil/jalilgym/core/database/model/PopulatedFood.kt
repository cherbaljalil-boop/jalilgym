package com.chdjalil.jalilgym.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.chdjalil.jalilgym.core.database.entity.DietPlanEntity
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.FoodLogEntryEntity
import com.chdjalil.jalilgym.core.database.entity.FoodServingEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealItemEntity
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodLogEntry
import com.chdjalil.jalilgym.core.model.FoodServing
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.SavedMeal
import com.chdjalil.jalilgym.core.model.SavedMealItem

data class PopulatedFood(
    @Embedded val food: FoodEntity,
    @Relation(parentColumn = "id", entityColumn = "foodId")
    val servings: List<FoodServingEntity>,
)

fun FoodEntity.nutrients() = Nutrients(
    kcal = kcalPer100g,
    proteinG = proteinPer100g,
    carbsG = carbsPer100g,
    fatG = fatPer100g,
    fiberG = fiberPer100g,
    sugarG = sugarPer100g,
    satFatG = satFatPer100g,
    sodiumMg = sodiumMgPer100g,
)

fun PopulatedFood.asExternalModel() = Food(
    id = food.id,
    name = food.name,
    brand = food.brand,
    barcode = food.barcode,
    source = food.source,
    per100g = food.nutrients(),
    halalStatus = food.halalStatus,
    isFavorite = food.isFavorite,
    servings = servings.sortedBy { it.grams }.map { FoodServing(it.label, it.grams) },
)

fun FoodLogEntryEntity.asExternalModel() = FoodLogEntry(
    id = id,
    date = date,
    meal = meal,
    foodId = foodId,
    name = nameSnapshot,
    quantityG = quantityG,
    nutrients = Nutrients(kcal, proteinG, carbsG, fatG, fiberG = fiberG, sugarG = sugarG, sodiumMg = sodiumMg),
)

data class PopulatedSavedMealItem(
    @Embedded val item: SavedMealItemEntity,
    @Relation(entity = FoodEntity::class, parentColumn = "foodId", entityColumn = "id")
    val food: PopulatedFood,
)

data class PopulatedSavedMeal(
    @Embedded val meal: SavedMealEntity,
    @Relation(entity = SavedMealItemEntity::class, parentColumn = "id", entityColumn = "savedMealId")
    val items: List<PopulatedSavedMealItem>,
)

fun PopulatedSavedMeal.asExternalModel() = SavedMeal(
    id = meal.id,
    name = meal.name,
    defaultMeal = meal.defaultMeal,
    items = items.sortedBy { it.item.position }.map { SavedMealItem(it.food.asExternalModel(), it.item.quantityG) },
)

fun DietPlanEntity.asExternalModel() = DietPlan(
    id = id,
    goal = goal,
    style = style,
    halalOnly = halalOnly,
    targets = MacroTargets(calories, proteinG, carbsG, fatG),
    isManualOverride = isManualOverride,
    startDate = startDate,
)
