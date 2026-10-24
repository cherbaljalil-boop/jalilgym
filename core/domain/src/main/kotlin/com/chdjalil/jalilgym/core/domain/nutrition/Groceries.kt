package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.Recipe

/** Something to buy; lines for the same food are merged by [key]. */
data class GroceryLine(val name: String, val foodId: String?, val grams: Double?) {
    val key: String get() = foodId ?: name.trim().lowercase()
}

/** Ingredients for [servings] of [recipe] (the whole recipe by default). */
fun recipeGroceryLines(recipe: Recipe, servings: Double = recipe.servings.toDouble()): List<GroceryLine> {
    val factor = if (recipe.servings > 0) servings / recipe.servings else 1.0
    return mergeGroceryLines(recipe.ingredients.map { GroceryLine(it.food.name, it.food.id, it.grams * factor) })
}

/** Everything a suggested day needs, with shared ingredients added up. */
fun dayPlanGroceryLines(plan: DayPlan): List<GroceryLine> =
    mergeGroceryLines(plan.meals.flatMap { recipeGroceryLines(it.recipe, it.servings) })

fun mergeGroceryLines(lines: List<GroceryLine>): List<GroceryLine> =
    lines.groupBy { it.key }.values.map { same ->
        val grams = same.mapNotNull { it.grams }.takeIf { it.isNotEmpty() }?.sum()
        same.first().copy(grams = grams)
    }
