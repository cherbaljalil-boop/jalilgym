package com.chdjalil.jalilgym.feature.nutrition.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGrams
import com.chdjalil.jalilgym.feature.nutrition.ui.formatKcal
import com.chdjalil.jalilgym.feature.nutrition.ui.labelRes

data class FoodSearchActions(
    val onBack: () -> Unit = {},
    val onOpenFood: (String) -> Unit = {},
    val onScan: () -> Unit = {},
    val onCreateFood: () -> Unit = {},
)

@Composable
internal fun FoodSearchScreen(actions: FoodSearchActions, viewModel: FoodSearchViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FoodSearchContent(uiState, actions, viewModel::setQuery, viewModel::searchOnline, viewModel::setHalalFilter)
}

@Composable
internal fun FoodSearchContent(
    uiState: FoodSearchUiState,
    actions: FoodSearchActions,
    onQuery: (String) -> Unit,
    onSearchOnline: () -> Unit,
    onHalalFilter: (HalalFilter) -> Unit,
) {
    Scaffold(
        topBar = {
            BackBar(
                stringResource(R.string.feature_nutrition_add_to, stringResource(uiState.meal.labelRes)),
                actions.onBack,
            ) {
                IconButton(onClick = actions.onScan) {
                    Icon(painterResource(JgIcons.Barcode), stringResource(R.string.feature_nutrition_scan_barcode))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQuery,
                placeholder = { Text(stringResource(R.string.feature_nutrition_search_hint)) },
                leadingIcon = { Icon(painterResource(JgIcons.Search), null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchOnline() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HalalFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.halalFilter == filter,
                        onClick = { onHalalFilter(filter) },
                        label = { Text(stringResource(filter.labelRes)) },
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                if (uiState.isSearching) {
                    items(uiState.results, key = { "r-${it.id}" }) { FoodRow(it) { actions.onOpenFood(it.id) } }
                    item { OnlineFooter(uiState, onSearchOnline, actions.onCreateFood) }
                } else {
                    section(R.string.feature_nutrition_favorites, uiState.favorites, "f", actions.onOpenFood)
                    section(R.string.feature_nutrition_recents, uiState.recents, "c", actions.onOpenFood)
                    if (uiState.favorites.isEmpty() && uiState.recents.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.feature_nutrition_search_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    item {
                        OutlinedButton(onClick = actions.onCreateFood, modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.feature_nutrition_create_food))
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.section(title: Int, foods: List<Food>, keyPrefix: String, onOpen: (String) -> Unit) {
    if (foods.isEmpty()) return
    item(key = "title-$keyPrefix") {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
        )
    }
    items(foods, key = { "$keyPrefix-${it.id}" }) { FoodRow(it) { onOpen(it.id) } }
}

@Composable
private fun OnlineFooter(uiState: FoodSearchUiState, onSearchOnline: () -> Unit, onCreateFood: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (uiState.hiddenByFilter > 0) {
            Text(
                pluralStringResource(R.plurals.feature_nutrition_hidden_by_filter, uiState.hiddenByFilter, uiState.hiddenByFilter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (uiState.online) {
            OnlineStatus.IDLE -> OutlinedButton(onClick = onSearchOnline) {
                Text(stringResource(R.string.feature_nutrition_search_off))
            }
            OnlineStatus.LOADING -> CircularProgressIndicator()
            OnlineStatus.DONE -> Text(
                stringResource(R.string.feature_nutrition_off_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OnlineStatus.OFFLINE -> Text(
                stringResource(R.string.feature_nutrition_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (uiState.results.isEmpty() && uiState.online != OnlineStatus.IDLE && uiState.online != OnlineStatus.LOADING) {
            Text(stringResource(R.string.feature_nutrition_no_results), style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onCreateFood) { Text(stringResource(R.string.feature_nutrition_create_food)) }
    }
}

@Composable
internal fun FoodRow(food: Food, onClick: () -> Unit) {
    val serving = food.servings.firstOrNull()
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(food.name) },
        supportingContent = {
            val per = if (serving != null) {
                stringResource(
                    R.string.feature_nutrition_per_serving,
                    formatKcal(food.nutrientsFor(serving.grams).kcal),
                    serving.label,
                )
            } else {
                stringResource(R.string.feature_nutrition_per_100g, formatKcal(food.per100g.kcal))
            }
            val protein = stringResource(R.string.feature_nutrition_protein_short, formatGrams(food.per100g.proteinG))
            Text(listOfNotNull(food.brand, per, protein).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            when (food.halalStatus) {
                HalalStatus.CERTIFIED, HalalStatus.USER_MARKED -> Text(
                    stringResource(R.string.feature_nutrition_halal_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                HalalStatus.FLAGGED -> Text(
                    stringResource(R.string.feature_nutrition_flagged_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                HalalStatus.UNKNOWN -> Unit
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}
