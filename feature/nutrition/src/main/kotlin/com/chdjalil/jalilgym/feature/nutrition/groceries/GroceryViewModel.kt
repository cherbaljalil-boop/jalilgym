package com.chdjalil.jalilgym.feature.nutrition.groceries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.GroceryRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.GroceryItem
import com.chdjalil.jalilgym.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroceryUiState(
    val isLoading: Boolean = true,
    val toBuy: List<GroceryItem> = emptyList(),
    val inCart: List<GroceryItem> = emptyList(),
    val units: UnitSystem = UnitSystem.METRIC,
)

@HiltViewModel
class GroceryViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<GroceryUiState> = combine(
        groceryRepository.observeItems(),
        preferencesRepository.userPreferences,
    ) { items, prefs ->
        val (inCart, toBuy) = items.partition { it.checked }
        GroceryUiState(isLoading = false, toBuy = toBuy, inCart = inCart, units = prefs.units)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroceryUiState())

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { groceryRepository.addItem(name) }
    }

    fun setChecked(id: String, checked: Boolean) {
        viewModelScope.launch { groceryRepository.setChecked(id, checked) }
    }

    fun delete(id: String) {
        viewModelScope.launch { groceryRepository.delete(id) }
    }

    fun clearChecked() {
        viewModelScope.launch { groceryRepository.clearChecked() }
    }
}
