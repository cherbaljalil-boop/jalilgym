package com.chdjalil.jalilgym.feature.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodServing
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.feature.nutrition.navigation.DATE_ARG
import com.chdjalil.jalilgym.feature.nutrition.navigation.ENTRY_ID_ARG
import com.chdjalil.jalilgym.feature.nutrition.navigation.FOOD_ID_ARG
import com.chdjalil.jalilgym.feature.nutrition.navigation.MEAL_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Amount as typed: a number of [serving]s, or grams when [serving] is null. */
data class AmountInput(val serving: FoodServing? = null, val text: String = "100")

data class FoodDetailUiState(
    val isLoading: Boolean = true,
    val food: Food? = null,
    val meal: MealType = MealType.SNACK,
    val amount: AmountInput = AmountInput(),
    val isEditing: Boolean = false,
    val done: Boolean = false,
) {
    val grams: Double?
        get() {
            val value = amount.text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: return null
            return amount.serving?.let { value * it.grams } ?: value
        }

    val nutrients: Nutrients? get() = grams?.let { g -> food?.nutrientsFor(g) }
}

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {

    private val foodId: String = checkNotNull(savedStateHandle[FOOD_ID_ARG])
    private val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>(DATE_ARG)))
    private val entryId: String? = savedStateHandle[ENTRY_ID_ARG]

    private val form = MutableStateFlow(
        FoodDetailUiState(meal = MealType.valueOf(checkNotNull(savedStateHandle.get<String>(MEAL_ARG))), isEditing = entryId != null),
    )

    val uiState: StateFlow<FoodDetailUiState> = combine(form, foodRepository.observeFood(foodId)) { state, food ->
        state.copy(food = food)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        viewModelScope.launch {
            // The logged amount when editing; otherwise one serving, or 100 g.
            val loggedGrams = entryId?.let { id -> nutritionRepository.observeDiary(date).first().entries.find { it.id == id }?.quantityG }
            val food = foodRepository.observeFood(foodId).filterNotNull().first()
            val serving = food.servings.firstOrNull()
            val amount = when {
                loggedGrams != null -> AmountInput(null, formatAmount(loggedGrams))
                serving != null -> AmountInput(serving, "1")
                else -> AmountInput(null, "100")
            }
            form.update { it.copy(isLoading = false, amount = amount) }
        }
    }

    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) "${value.toLong()}" else "%.1f".format(java.util.Locale.ROOT, value)

    fun setAmountText(text: String) = form.update { it.copy(amount = it.amount.copy(text = text)) }

    /** Switching unit keeps the same weight. */
    fun selectServing(serving: FoodServing?) = form.update { state ->
        val grams = state.grams
        val text = when {
            grams == null -> state.amount.text
            serving == null -> formatAmount(grams)
            else -> formatAmount(grams / serving.grams)
        }
        state.copy(amount = AmountInput(serving, text))
    }

    fun selectMeal(meal: MealType) = form.update { it.copy(meal = meal) }

    fun toggleFavorite() {
        val food = uiState.value.food ?: return
        viewModelScope.launch { foodRepository.setFavorite(food.id, !food.isFavorite) }
    }

    fun setHalalMark(marked: Boolean) {
        viewModelScope.launch { foodRepository.setUserMarkedHalal(foodId, marked) }
    }

    fun save() {
        val state = uiState.value
        val grams = state.grams ?: return
        viewModelScope.launch {
            if (entryId != null) {
                nutritionRepository.updateEntryQuantity(entryId, grams)
            } else {
                nutritionRepository.logFood(date, state.meal, foodId, grams)
            }
            form.update { it.copy(done = true) }
        }
    }
}
