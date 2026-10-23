package com.chdjalil.jalilgym.feature.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.core.model.FoodDraft
import com.chdjalil.jalilgym.core.model.FoodServing
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.feature.nutrition.navigation.BARCODE_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomFoodForm(
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    /** Label values are per this many grams (100 by default, or one serving). */
    val basisGrams: String = "100",
    /** Optional serving name, e.g. "1 bar"; its size is [basisGrams] when not 100. */
    val servingLabel: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val fiber: String = "",
    val sugar: String = "",
    val satFat: String = "",
    val sodiumMg: String = "",
    val markedHalal: Boolean = false,
) {
    private fun String.num() = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

    private val basis: Double? get() = basisGrams.num()?.takeIf { it > 0 }

    /** Valid when it has a name, a basis and energy; missing macros count as 0. */
    fun toDraft(): FoodDraft? {
        val basis = basis ?: return null
        val kcal = kcal.num() ?: return null
        if (name.isBlank()) return null
        val f = 100.0 / basis
        val per100 = Nutrients(
            kcal = kcal * f,
            proteinG = (protein.num() ?: 0.0) * f,
            carbsG = (carbs.num() ?: 0.0) * f,
            fatG = (fat.num() ?: 0.0) * f,
            fiberG = fiber.num()?.times(f),
            sugarG = sugar.num()?.times(f),
            satFatG = satFat.num()?.times(f),
            sodiumMg = sodiumMg.num()?.times(f),
        )
        val servings = buildList {
            if (basis != 100.0 || servingLabel.isNotBlank()) {
                add(FoodServing(servingLabel.trim().ifEmpty { "1 serving" }, basis))
            }
        }
        return FoodDraft(
            name = name,
            brand = brand,
            barcode = barcode,
            per100g = per100,
            servings = servings,
            markedHalal = markedHalal,
        )
    }
}

data class CustomFoodUiState(val form: CustomFoodForm = CustomFoodForm(), val savedFoodId: String? = null) {
    val canSave: Boolean get() = form.toDraft() != null
}

@HiltViewModel
class CustomFoodViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomFoodUiState(CustomFoodForm(barcode = savedStateHandle.get<String>(BARCODE_ARG).orEmpty())))
    val uiState: StateFlow<CustomFoodUiState> = _uiState.asStateFlow()

    fun update(transform: CustomFoodForm.() -> CustomFoodForm) = _uiState.update { it.copy(form = it.form.transform()) }

    fun save() {
        val draft = _uiState.value.form.toDraft() ?: return
        viewModelScope.launch {
            val id = foodRepository.saveCustomFood(draft)
            _uiState.update { it.copy(savedFoodId = id) }
        }
    }
}
