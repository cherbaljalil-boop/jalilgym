package com.chdjalil.jalilgym.feature.nutrition.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.SavedMeal
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.navigation.DATE_ARG
import com.chdjalil.jalilgym.feature.nutrition.navigation.MEAL_ARG
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGrams
import com.chdjalil.jalilgym.feature.nutrition.ui.formatKcal
import com.chdjalil.jalilgym.feature.nutrition.ui.labelRes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedMealsUiState(val isLoading: Boolean = true, val meal: MealType, val meals: List<SavedMeal> = emptyList())

@HiltViewModel
class SavedMealsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {
    private val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle.get<String>(DATE_ARG)))
    private val meal: MealType = MealType.valueOf(checkNotNull(savedStateHandle.get<String>(MEAL_ARG)))

    val uiState: StateFlow<SavedMealsUiState> = nutritionRepository.observeSavedMeals()
        .map { SavedMealsUiState(isLoading = false, meal = meal, meals = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedMealsUiState(meal = meal))

    fun log(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            nutritionRepository.logSavedMeal(id, date, meal)
            onDone()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { nutritionRepository.deleteSavedMeal(id) }
    }
}

@Composable
internal fun SavedMealsScreen(onBack: () -> Unit, viewModel: SavedMealsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_nutrition_saved_meals), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (!uiState.isLoading && uiState.meals.isEmpty()) {
                Text(
                    stringResource(R.string.feature_nutrition_saved_meals_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.meals, key = { it.id }) { saved ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp, end = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(saved.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(
                                            R.string.feature_nutrition_saved_meal_summary,
                                            formatKcal(saved.nutrients.kcal),
                                            formatGrams(saved.nutrients.proteinG),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.delete(saved.id) }) {
                                    Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_nutrition_delete_saved, saved.name))
                                }
                            }
                            Text(
                                saved.items.joinToString(", ") { it.food.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { viewModel.log(saved.id, onBack) }) {
                                Text(stringResource(R.string.feature_nutrition_log_to, stringResource(uiState.meal.labelRes)))
                            }
                        }
                    }
                }
            }
        }
    }
}
