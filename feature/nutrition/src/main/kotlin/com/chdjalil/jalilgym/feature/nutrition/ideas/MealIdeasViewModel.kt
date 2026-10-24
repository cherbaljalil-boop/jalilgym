package com.chdjalil.jalilgym.feature.nutrition.ideas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.nutrition.DayPlan
import com.chdjalil.jalilgym.core.domain.nutrition.suggestDay
import com.chdjalil.jalilgym.core.domain.nutrition.swapMeal
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.RecipeRepository
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealIdeasUiState(
    val isLoading: Boolean = true,
    /** Null until nutrition setup is done. */
    val targets: MacroTargets? = null,
    val plan: DayPlan? = null,
    /** Indexes of meals already logged today from this plan. */
    val logged: Set<Int> = emptySet(),
)

@HiltViewModel
class MealIdeasViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val nutritionRepository: NutritionRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealIdeasUiState())
    val uiState: StateFlow<MealIdeasUiState> = _uiState.asStateFlow()

    private var recipes: List<Recipe> = emptyList()
    private var swaps = 0

    /** Keeps the same day across configuration changes and process death. */
    private var seed: Long
        get() = savedStateHandle[SEED_KEY] ?: System.currentTimeMillis().also { savedStateHandle[SEED_KEY] = it }
        set(value) {
            savedStateHandle[SEED_KEY] = value
        }

    init {
        viewModelScope.launch {
            combine(nutritionRepository.observeActivePlan(), recipeRepository.observeRecipes()) { plan, recipes -> plan?.targets to recipes }
                .collect { (targets, all) ->
                    // Only recipes tagged with meals can be suggested (the built-in ones).
                    recipes = all.filter { it.mealTags.isNotEmpty() }
                    val changed = targets != _uiState.value.targets
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            targets = targets,
                            plan = if (changed || it.plan == null) targets?.let { t -> suggestDay(t, recipes, Random(seed)) } else it.plan,
                            logged = if (changed) emptySet() else it.logged,
                        )
                    }
                }
        }
    }

    /** A fresh day of suggestions. */
    fun newDay() {
        seed = Random.nextLong()
        val targets = _uiState.value.targets ?: return
        _uiState.update { it.copy(plan = suggestDay(targets, recipes, Random(seed)), logged = emptySet()) }
    }

    fun swap(index: Int) {
        val state = _uiState.value
        val plan = state.plan ?: return
        val targets = state.targets ?: return
        _uiState.update { it.copy(plan = swapMeal(plan, index, targets, recipes, Random(seed + ++swaps)), logged = it.logged - index) }
    }

    /** Logs one suggested meal to today's diary, at the suggested portion. */
    fun log(index: Int) {
        val meal = _uiState.value.plan?.meals?.getOrNull(index) ?: return
        if (index in _uiState.value.logged) return
        _uiState.update { it.copy(logged = it.logged + index) }
        viewModelScope.launch {
            nutritionRepository.logFood(LocalDate.now(), meal.meal, recipeRepository.foodIdFor(meal.recipe.id), meal.grams)
        }
    }

    fun logAll() {
        _uiState.value.plan?.meals?.indices?.forEach(::log)
    }

    private companion object {
        const val SEED_KEY = "seed"
    }
}
