package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeIngredient
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GroceriesTest {

    private fun food(id: String) =
        Food(id, id.replaceFirstChar(Char::uppercase), null, null, FoodSource.SEED, Nutrients(100.0, 10.0, 10.0, 2.0), HalalStatus.UNKNOWN)

    private val rice = food("rice")
    private val chicken = food("chicken")
    private val bowl = Recipe("bowl", "Chicken bowl", 2, listOf(RecipeIngredient(chicken, 300.0), RecipeIngredient(rice, 160.0)))
    private val stirFry = Recipe("fry", "Stir fry", 4, listOf(RecipeIngredient(chicken, 600.0), RecipeIngredient(food("peppers"), 400.0)))

    @Test
    fun `whole recipe by default, scaled for other servings`() {
        assertThat(recipeGroceryLines(bowl)).containsExactly(
            GroceryLine("Chicken", "chicken", 300.0),
            GroceryLine("Rice", "rice", 160.0),
        )
        assertThat(recipeGroceryLines(bowl, servings = 1.0).first { it.foodId == "rice" }.grams).isEqualTo(80.0)
    }

    @Test
    fun `a day adds up ingredients shared between meals`() {
        val plan = DayPlan(
            listOf(
                SuggestedMeal(MealType.LUNCH, bowl, 1.5),
                SuggestedMeal(MealType.DINNER, stirFry, 1.0),
            ),
        )
        val lines = dayPlanGroceryLines(plan).associateBy { it.foodId }
        assertThat(lines.keys).containsExactly("chicken", "rice", "peppers")
        assertThat(lines.getValue("chicken").grams).isEqualTo(225.0 + 150.0)
        assertThat(lines.getValue("rice").grams).isEqualTo(120.0)
    }

    @Test
    fun `hand-typed lines merge by name and keep no amount`() {
        val merged = mergeGroceryLines(listOf(GroceryLine("Milk", null, null), GroceryLine(" milk ", null, null)))
        assertThat(merged).containsExactly(GroceryLine("Milk", null, null))
    }
}
