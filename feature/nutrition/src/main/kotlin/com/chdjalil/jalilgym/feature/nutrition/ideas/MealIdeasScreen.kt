package com.chdjalil.jalilgym.feature.nutrition.ideas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.designsystem.theme.JgTheme
import com.chdjalil.jalilgym.core.domain.nutrition.SuggestedMeal
import com.chdjalil.jalilgym.core.ui.EntryCard
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.groceries.GroceriesAddedNote
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGrams
import com.chdjalil.jalilgym.feature.nutrition.ui.formatKcal
import com.chdjalil.jalilgym.feature.nutrition.ui.labelRes

@Composable
internal fun MealIdeasScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onOpenGroceries: () -> Unit,
    onOpenSetup: () -> Unit,
    viewModel: MealIdeasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MealIdeasContent(
        uiState,
        onBack,
        onOpenRecipe,
        onOpenSetup,
        viewModel::swap,
        viewModel::log,
        viewModel::logAll,
        viewModel::newDay,
        onAddToGroceries = viewModel::addDayToGroceries,
        onOpenGroceries = onOpenGroceries,
    )
}

@Composable
internal fun MealIdeasContent(
    uiState: MealIdeasUiState,
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit = {},
    onOpenSetup: () -> Unit = {},
    onSwap: (Int) -> Unit = {},
    onLog: (Int) -> Unit = {},
    onLogAll: () -> Unit = {},
    onNewDay: () -> Unit = {},
    onAddToGroceries: () -> Unit = {},
    onOpenGroceries: () -> Unit = {},
) {
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_nutrition_meal_ideas), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val targets = uiState.targets
            val plan = uiState.plan
            when {
                targets == null -> EntryCard(
                    title = stringResource(R.string.feature_nutrition_setup_title),
                    description = stringResource(R.string.feature_nutrition_ideas_need_targets),
                    icon = JgIcons.Nutrition,
                    onClick = onOpenSetup,
                )
                plan == null -> Text(stringResource(R.string.feature_nutrition_ideas_none), style = MaterialTheme.typography.bodyLarge)
                else -> {
                    val totals = plan.totals
                    Text(
                        stringResource(
                            R.string.feature_nutrition_ideas_totals,
                            formatKcal(totals.kcal),
                            targets.calories,
                            formatGrams(totals.proteinG),
                            targets.proteinG,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(if (plan.fits(targets)) R.string.feature_nutrition_ideas_fits else R.string.feature_nutrition_ideas_close),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (plan.fits(targets)) JgTheme.extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    plan.meals.forEachIndexed { index, meal ->
                        MealCard(meal, logged = index in uiState.logged, onOpen = { onOpenRecipe(meal.recipe.id) }, onSwap = { onSwap(index) }, onLog = {
                            onLog(index)
                        })
                    }
                    JgPrimaryButton(onClick = onLogAll, enabled = uiState.logged.size < plan.meals.size, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.feature_nutrition_ideas_log_all))
                    }
                    OutlinedButton(onClick = onNewDay, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.feature_nutrition_ideas_new_day))
                    }
                    OutlinedButton(onClick = onAddToGroceries, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.feature_nutrition_groceries_add_day))
                    }
                    uiState.groceriesAdded?.let { GroceriesAddedNote(it, onOpenGroceries) }
                    Text(
                        stringResource(R.string.feature_nutrition_ideas_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: SuggestedMeal, logged: Boolean, onOpen: () -> Unit, onSwap: () -> Unit, onLog: () -> Unit) {
    val n = meal.nutrients
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                stringResource(meal.meal.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(meal.recipe.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.feature_nutrition_ideas_meal_line,
                    formatServings(meal.servings),
                    formatKcal(n.kcal),
                    formatGrams(n.proteinG),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSwap) { Text(stringResource(R.string.feature_nutrition_ideas_swap)) }
                TextButton(onClick = onLog, enabled = !logged) {
                    Text(stringResource(if (logged) R.string.feature_nutrition_ideas_logged else R.string.feature_nutrition_ideas_log))
                }
            }
        }
    }
}

private fun formatServings(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}" else "$value"
