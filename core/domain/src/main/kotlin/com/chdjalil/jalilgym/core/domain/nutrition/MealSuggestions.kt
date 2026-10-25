package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Recipe
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/** Share of the day's calories each meal aims for. */
val MEAL_SHARES: Map<MealType, Double> = linkedMapOf(
    MealType.BREAKFAST to 0.25,
    MealType.LUNCH to 0.35,
    MealType.DINNER to 0.30,
    MealType.SNACK to 0.10,
)

/** Portion sizes a suggestion may use, in servings. */
val SERVING_STEPS: List<Double> = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5)

/** A day lands when calories are within ±5% and protein within ±10 g (docs/BLUEPRINT.md §3.7). */
const val PLAN_CALORIE_TOLERANCE = 0.05
const val PLAN_PROTEIN_TOLERANCE_G = 10.0

data class SuggestedMeal(val meal: MealType, val recipe: Recipe, val servings: Double) {
    val nutrients: Nutrients get() = recipe.perServing * servings
    val grams: Double get() = recipe.servingGrams * servings
}

data class DayPlan(val meals: List<SuggestedMeal>) {
    val totals: Nutrients get() = meals.fold(Nutrients.ZERO) { sum, m -> sum + m.nutrients }

    fun fits(targets: MacroTargets): Boolean =
        abs(totals.kcal - targets.calories) <= targets.calories * PLAN_CALORIE_TOLERANCE &&
            abs(totals.proteinG - targets.proteinG) <= PLAN_PROTEIN_TOLERANCE_G
}

/** Diet tags shown on recipes: HIGH_PROTEIN (≥30% of kcal), CUT (≤450 kcal) or BULK (≥650 kcal) per serving. */
fun dietTags(perServing: Nutrients): List<String> = buildList {
    if (perServing.kcal > 0 && perServing.proteinG * KCAL_PER_G_PROTEIN / perServing.kcal >= 0.30) add("HIGH_PROTEIN")
    if (perServing.kcal <= 450) add("CUT")
    if (perServing.kcal >= 650) add("BULK")
}

/** How far a day is from the targets; zero inside the tolerance. */
internal fun planError(totals: Nutrients, targets: MacroTargets): Double {
    val kcal = max(0.0, abs(totals.kcal - targets.calories) - targets.calories * PLAN_CALORIE_TOLERANCE) / targets.calories
    val protein = max(0.0, abs(totals.proteinG - targets.proteinG) - PLAN_PROTEIN_TOLERANCE_G) / max(1, targets.proteinG)
    // Small pull toward the exact target so ties prefer closer days.
    val closeness = abs(totals.kcal - targets.calories) / targets.calories * 0.01
    return kcal + protein + closeness
}

private fun candidates(recipes: List<Recipe>, meal: MealType, exclude: Set<String>) =
    recipes.filter { meal.name in it.mealTags && it.id !in exclude && it.servingGrams > 0 }

/** Adjusts portions one step at a time while that brings the day closer to the targets. */
private fun refinePortions(plan: List<SuggestedMeal>, targets: MacroTargets, locked: Set<Int> = emptySet()): List<SuggestedMeal> {
    var current = plan
    var error = planError(DayPlan(current).totals, targets)
    while (true) {
        var best: List<SuggestedMeal>? = null
        current.indices.filter { it !in locked }.forEach { i ->
            SERVING_STEPS.forEach { step ->
                if (step == current[i].servings) return@forEach
                val tried = current.toMutableList().also { it[i] = it[i].copy(servings = step) }
                val e = planError(DayPlan(tried).totals, targets)
                if (e < error - 1e-9) {
                    error = e
                    best = tried
                }
            }
        }
        current = best ?: return current
    }
}

/**
 * Suggests a day of meals from [recipes] (docs/BLUEPRINT.md §3.7): each meal
 * gets a share of the calories, a recipe and portion close to that share are
 * picked (randomly among the closest few, for variety), then portions are
 * tuned until the day is within ±5% of calories and ±10 g of protein, or as
 * close as the recipes allow. The same [random] seed gives the same day.
 */
fun suggestDay(targets: MacroTargets, recipes: List<Recipe>, random: Random, attempts: Int = 24): DayPlan? {
    var best: List<SuggestedMeal>? = null
    var bestError = Double.MAX_VALUE
    repeat(attempts) {
        val meals = MEAL_SHARES.mapNotNull { (meal, share) ->
            val kcal = targets.calories * share
            val protein = targets.proteinG * share
            candidates(recipes, meal, emptySet())
                .flatMap { recipe -> SERVING_STEPS.map { SuggestedMeal(meal, recipe, it) } }
                .sortedBy { abs(it.nutrients.kcal - kcal) / kcal + abs(it.nutrients.proteinG - protein) / max(protein, 1.0) * 0.5 }
                .take(3)
                .randomOrNull(random)
        }
        if (meals.isEmpty()) return null
        val refined = refinePortions(meals, targets)
        val error = planError(DayPlan(refined).totals, targets)
        if (error < bestError) {
            bestError = error
            best = refined
        }
        if (DayPlan(refined).fits(targets)) return DayPlan(refined)
    }
    return best?.let(::DayPlan)
}

/**
 * Replaces the meal at [index] with a different recipe, keeping the other
 * meals' recipes and re-tuning portions to stay on target.
 */
fun swapMeal(plan: DayPlan, index: Int, targets: MacroTargets, recipes: List<Recipe>, random: Random): DayPlan {
    val current = plan.meals[index]
    val options = candidates(recipes, current.meal, setOf(current.recipe.id)).shuffled(random)
    if (options.isEmpty()) return plan
    val best = options.take(6).map { recipe ->
        val start = plan.meals.toMutableList().also { it[index] = SuggestedMeal(current.meal, recipe, 1.0) }
        refinePortions(start, targets)
    }.minBy { planError(DayPlan(it).totals, targets) }
    return DayPlan(best)
}
