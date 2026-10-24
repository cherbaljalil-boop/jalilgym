package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeIngredient
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class MealSuggestionsTest {

    /** A one-ingredient recipe whose single serving has exactly these numbers. */
    private fun recipe(id: String, kcal: Double, protein: Double, vararg meals: MealType): Recipe {
        val food = Food(id, id, null, null, FoodSource.SEED, Nutrients(kcal, protein, (kcal - protein * 4) / 8, (kcal - protein * 4) / 18), HalalStatus.UNKNOWN)
        return Recipe(id, id, 1, listOf(RecipeIngredient(food, 100.0)), mealTags = meals.map { it.name })
    }

    private val recipes = listOf(
        recipe("oats", 380.0, 15.0, MealType.BREAKFAST),
        recipe("eggs", 350.0, 35.0, MealType.BREAKFAST),
        recipe("skyr", 380.0, 27.0, MealType.BREAKFAST, MealType.SNACK),
        recipe("chicken-bowl", 600.0, 55.0, MealType.LUNCH, MealType.DINNER),
        recipe("lentil-soup", 340.0, 18.0, MealType.LUNCH, MealType.DINNER),
        recipe("chili", 640.0, 49.0, MealType.LUNCH, MealType.DINNER),
        recipe("salmon", 570.0, 41.0, MealType.DINNER),
        recipe("wrap", 400.0, 45.0, MealType.LUNCH),
        recipe("shake", 270.0, 34.0, MealType.SNACK),
        recipe("apple-pb", 210.0, 6.0, MealType.SNACK),
    )

    @Test
    fun daysLandWithinToleranceForTypicalTargets() {
        listOf(MacroTargets(1800, 150, 170, 60), MacroTargets(2200, 176, 240, 61), MacroTargets(3000, 180, 380, 84)).forEach { targets ->
            val plan = suggestDay(targets, recipes, Random(7))!!
            assertThat(plan.meals.map { it.meal }).containsExactly(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).inOrder()
            assertThat(plan.fits(targets)).isTrue()
            plan.meals.forEach { assertThat(it.recipe.mealTags).contains(it.meal.name) }
        }
    }

    @Test
    fun sameSeedSameDayAndSwapsChangeOnlyOneRecipe() {
        val targets = MacroTargets(2200, 176, 240, 61)
        val plan = suggestDay(targets, recipes, Random(42))!!
        assertThat(suggestDay(targets, recipes, Random(42))).isEqualTo(plan)

        val swapped = swapMeal(plan, 1, targets, recipes, Random(1))
        assertThat(swapped.meals[1].recipe.id).isNotEqualTo(plan.meals[1].recipe.id)
        assertThat(swapped.meals.map { it.recipe.id }.filterIndexed { i, _ -> i != 1 })
            .isEqualTo(plan.meals.map { it.recipe.id }.filterIndexed { i, _ -> i != 1 })
    }

    @Test
    fun noRecipesNoPlan() {
        assertThat(suggestDay(MacroTargets(2000, 150, 200, 70), emptyList(), Random(1))).isNull()
    }

    @Test
    fun tags() {
        assertThat(dietTags(Nutrients(400.0, 40.0, 30.0, 10.0))).containsExactly("HIGH_PROTEIN", "CUT")
        assertThat(dietTags(Nutrients(700.0, 30.0, 90.0, 20.0))).containsExactly("BULK")
    }
}
