package com.chdjalil.jalilgym.core.model

import java.time.LocalDate

enum class DietGoal { CUT, LEAN_BULK, MAINTAIN }

/** High-protein is a modifier on any goal (docs/BLUEPRINT.md §3.7). */
enum class DietStyle { BALANCED, HIGH_PROTEIN }

/** RECIPE foods are generated from a recipe and kept in sync with it. */
enum class FoodSource { SEED, OPEN_FOOD_FACTS, CUSTOM, RECIPE }

/** Never inferred as certified without a label. */
enum class HalalStatus { CERTIFIED, FLAGGED, USER_MARKED, UNKNOWN }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

enum class Sex { MALE, FEMALE }

/** Multipliers on BMR (docs/BLUEPRINT.md §3.7). */
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    VERY_ACTIVE(1.725),
    EXTRA_ACTIVE(1.9),
}

/** What nutrition setup asks for; weight comes from the body-weight log. */
data class BodyProfile(
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val activityLevel: ActivityLevel,
)

enum class HalalFilter { ALL, HIDE_FLAGGED, CERTIFIED_ONLY }

/** Daily targets. */
data class MacroTargets(val calories: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

data class DietPlan(
    val id: String,
    val goal: DietGoal,
    val style: DietStyle,
    val halalOnly: Boolean,
    val targets: MacroTargets,
    val isManualOverride: Boolean,
    val startDate: LocalDate,
)

/** Energy and nutrients; micronutrients are null when the source doesn't say. */
data class Nutrients(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val satFatG: Double? = null,
    val sodiumMg: Double? = null,
) {
    operator fun plus(other: Nutrients) = Nutrients(
        kcal = kcal + other.kcal,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
        fiberG = fiberG.plusNullable(other.fiberG),
        sugarG = sugarG.plusNullable(other.sugarG),
        satFatG = satFatG.plusNullable(other.satFatG),
        sodiumMg = sodiumMg.plusNullable(other.sodiumMg),
    )

    operator fun times(factor: Double) = Nutrients(
        kcal * factor,
        proteinG * factor,
        carbsG * factor,
        fatG * factor,
        fiberG?.times(factor),
        sugarG?.times(factor),
        satFatG?.times(factor),
        sodiumMg?.times(factor),
    )

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0)
    }
}

/** Missing on both sides stays missing; known on one side counts. */
private fun Double?.plusNullable(other: Double?): Double? = if (this == null && other == null) null else (this ?: 0.0) + (other ?: 0.0)

data class FoodServing(val label: String, val grams: Double)

data class Food(
    val id: String,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val source: FoodSource,
    val per100g: Nutrients,
    val halalStatus: HalalStatus,
    val isFavorite: Boolean = false,
    val servings: List<FoodServing> = emptyList(),
) {
    fun nutrientsFor(grams: Double): Nutrients = per100g * (grams / 100.0)
}

/** A custom food; nutrients are per 100 g. */
data class FoodDraft(
    val id: String? = null,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val per100g: Nutrients,
    val servings: List<FoodServing> = emptyList(),
    val markedHalal: Boolean = false,
)

data class FoodLogEntry(
    val id: String,
    val date: LocalDate,
    val meal: MealType,
    val foodId: String?,
    val name: String,
    val quantityG: Double,
    /** Snapshot taken when logged. */
    val nutrients: Nutrients,
)

data class DiaryDay(val date: LocalDate, val entries: List<FoodLogEntry>) {
    val totals: Nutrients get() = entries.fold(Nutrients.ZERO) { sum, e -> sum + e.nutrients }

    fun entriesFor(meal: MealType): List<FoodLogEntry> = entries.filter { it.meal == meal }
}

data class SavedMealItem(val food: Food, val quantityG: Double)

data class SavedMeal(val id: String, val name: String, val defaultMeal: MealType?, val items: List<SavedMealItem>) {
    val nutrients: Nutrients get() = items.fold(Nutrients.ZERO) { sum, item -> sum + item.food.nutrientsFor(item.quantityG) }
}

data class RecipeIngredient(val food: Food, val grams: Double) {
    val nutrients: Nutrients get() = food.nutrientsFor(grams)
}

data class Recipe(
    val id: String,
    val name: String,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,
    val instructions: List<String> = emptyList(),
    /** Meal types this suits (MealType names), for suggestions. */
    val mealTags: List<String> = emptyList(),
    /** HIGH_PROTEIN, CUT or BULK, for suggestions. */
    val dietTags: List<String> = emptyList(),
    /** Built-in recipes are written without pork, alcohol or gelatin. */
    val isHalalFriendly: Boolean = false,
    val isBuiltIn: Boolean = false,
) {
    val totalGrams: Double get() = ingredients.sumOf { it.grams }

    val total: Nutrients get() = ingredients.fold(Nutrients.ZERO) { sum, i -> sum + i.nutrients }

    val perServing: Nutrients get() = total * (1.0 / servings.coerceAtLeast(1))

    val servingGrams: Double get() = totalGrams / servings.coerceAtLeast(1)
}

/** A recipe being edited: ingredients as food id and grams. */
data class RecipeDraft(
    val id: String? = null,
    val name: String,
    val servings: Int,
    val ingredients: List<Pair<String, Double>>,
    val instructions: List<String> = emptyList(),
)

/** One line on the grocery list; [grams] is null for items typed in by hand. */
data class GroceryItem(
    val id: String,
    val name: String,
    val foodId: String?,
    val grams: Double?,
    val checked: Boolean,
)
