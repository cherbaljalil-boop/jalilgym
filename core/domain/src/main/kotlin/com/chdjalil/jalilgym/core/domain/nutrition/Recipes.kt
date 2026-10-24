package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.Recipe

/**
 * A recipe's halal status from its ingredients: flagged if any is flagged,
 * marked if every one is labeled or marked, otherwise unknown. Never more
 * certain than its least certain ingredient.
 */
fun recipeHalalStatus(recipe: Recipe): HalalStatus {
    val statuses = recipe.ingredients.map { it.food.halalStatus }
    return when {
        statuses.isEmpty() -> HalalStatus.UNKNOWN
        HalalStatus.FLAGGED in statuses -> HalalStatus.FLAGGED
        statuses.all { it == HalalStatus.CERTIFIED || it == HalalStatus.USER_MARKED } -> HalalStatus.USER_MARKED
        else -> HalalStatus.UNKNOWN
    }
}
