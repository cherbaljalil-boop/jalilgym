package com.chdjalil.jalilgym.feature.nutrition.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.core.domain.repository.RecipeRepository
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.core.model.RecipeDraft
import com.chdjalil.jalilgym.core.model.RecipeIngredient
import com.chdjalil.jalilgym.feature.nutrition.navigation.RECIPE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipesUiState(val isLoading: Boolean = true, val mine: List<Recipe> = emptyList(), val builtIn: List<Recipe> = emptyList())

@HiltViewModel
class RecipesViewModel @Inject constructor(recipeRepository: RecipeRepository) : ViewModel() {
    val uiState: StateFlow<RecipesUiState> = recipeRepository.observeRecipes()
        .map { list -> RecipesUiState(false, list.filterNot { it.isBuiltIn }, list.filter { it.isBuiltIn }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())
}

/** An ingredient row as typed. */
data class EditableIngredient(val food: Food, val grams: String) {
    val gramsValue: Double? get() = grams.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
}

data class RecipeEditorUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val servings: Int = 1,
    val ingredients: List<EditableIngredient> = emptyList(),
    val instructions: String = "",
    /** Editing a built-in recipe saves a copy. */
    val isBuiltIn: Boolean = false,
    val isNew: Boolean = true,
    val ingredientQuery: String = "",
    val ingredientResults: List<Food> = emptyList(),
    /** The recipe's food, for "log a serving"; null until it's saved. */
    val foodId: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    private val asRecipe: Recipe
        get() = Recipe(
            id = "",
            name = name,
            servings = servings,
            ingredients = ingredients.mapNotNull { i -> i.gramsValue?.let { RecipeIngredient(i.food, it) } },
        )

    val perServing: Nutrients get() = asRecipe.perServing

    val servingGrams: Double get() = asRecipe.servingGrams

    val canSave: Boolean get() = name.isNotBlank() && ingredients.any { it.gramsValue != null }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val recipeId: String? = savedStateHandle[RECIPE_ID_ARG]
    private val form = MutableStateFlow(RecipeEditorUiState(isLoading = recipeId != null, isNew = recipeId == null))
    val uiState: StateFlow<RecipeEditorUiState> = form.asStateFlow()

    init {
        recipeId?.let { id ->
            viewModelScope.launch {
                val recipe = recipeRepository.observeRecipe(id).filterNotNull().first()
                form.update {
                    it.copy(
                        isLoading = false,
                        name = recipe.name,
                        servings = recipe.servings,
                        ingredients = recipe.ingredients.map { i -> EditableIngredient(i.food, formatGrams(i.grams)) },
                        instructions = recipe.instructions.joinToString("\n"),
                        isBuiltIn = recipe.isBuiltIn,
                        foodId = recipeRepository.foodIdFor(id),
                    )
                }
            }
        }
        viewModelScope.launch {
            form.map { it.ingredientQuery.trim() }
                .distinctUntilChanged()
                .flatMapLatest { q -> if (q.length < 2) flowOf(emptyList()) else foodRepository.searchLocal(q) }
                .collect { results -> form.update { it.copy(ingredientResults = results.take(MAX_INGREDIENT_RESULTS)) } }
        }
    }

    private fun formatGrams(value: Double) = if (value % 1.0 == 0.0) "${value.toLong()}" else "$value"

    fun setName(value: String) = form.update { it.copy(name = value) }

    fun setServings(value: Int) = form.update { it.copy(servings = value.coerceIn(1, MAX_SERVINGS)) }

    fun setInstructions(value: String) = form.update { it.copy(instructions = value) }

    fun setIngredientQuery(value: String) = form.update { it.copy(ingredientQuery = value) }

    /** Adds one serving of [food] (or 100 g when it has none). */
    fun addIngredient(food: Food) = form.update {
        val grams = food.servings.firstOrNull()?.grams ?: 100.0
        it.copy(ingredients = it.ingredients + EditableIngredient(food, formatGrams(grams)), ingredientQuery = "")
    }

    fun setIngredientGrams(index: Int, grams: String) = form.update {
        it.copy(ingredients = it.ingredients.mapIndexed { i, row -> if (i == index) row.copy(grams = grams) else row })
    }

    fun removeIngredient(index: Int) = form.update { it.copy(ingredients = it.ingredients.filterIndexed { i, _ -> i != index }) }

    fun save() {
        val state = form.value
        if (!state.canSave) return
        viewModelScope.launch {
            val id = recipeRepository.saveRecipe(
                RecipeDraft(
                    // Built-in recipes are copied, never changed.
                    id = recipeId.takeUnless { state.isBuiltIn },
                    name = state.name,
                    servings = state.servings,
                    ingredients = state.ingredients.mapNotNull { row -> row.gramsValue?.let { row.food.id to it } },
                    instructions = state.instructions.lines(),
                ),
            )
            form.update { it.copy(saved = true, foodId = recipeRepository.foodIdFor(id)) }
        }
    }

    fun delete() {
        val id = recipeId ?: return
        viewModelScope.launch {
            recipeRepository.deleteRecipe(id)
            form.update { it.copy(deleted = true) }
        }
    }

    private companion object {
        const val MAX_INGREDIENT_RESULTS = 8
        const val MAX_SERVINGS = 50
    }
}
