package com.chdjalil.jalilgym.feature.nutrition.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.component.JgPrimaryButton
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.Recipe
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.groceries.GroceriesAddedNote
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGrams
import com.chdjalil.jalilgym.feature.nutrition.ui.formatKcal

@Composable
internal fun RecipesScreen(
    onBack: () -> Unit,
    onOpenRecipe: (String?) -> Unit,
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecipesContent(uiState, onBack, onOpenRecipe)
}

@Composable
internal fun RecipesContent(uiState: RecipesUiState, onBack: () -> Unit, onOpenRecipe: (String?) -> Unit) {
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_nutrition_recipes), onBack) },
        floatingActionButton = {
            val label = stringResource(R.string.feature_nutrition_new_recipe)
            ExtendedFloatingActionButton(
                onClick = { onOpenRecipe(null) },
                // The text alone isn't exposed to TalkBack here; label the button itself.
                modifier = Modifier.semantics { contentDescription = label },
                text = { Text(label) },
                icon = { Icon(painterResource(JgIcons.Add), null) },
            )
        },
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
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            section(R.string.feature_nutrition_my_recipes, uiState.mine, onOpenRecipe)
            if (uiState.mine.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_nutrition_recipes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            section(R.string.feature_nutrition_built_in_recipes, uiState.builtIn, onOpenRecipe)
        }
    }
}

private fun LazyListScope.section(title: Int, recipes: List<Recipe>, onOpen: (String?) -> Unit) {
    item(key = "title-$title") {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
        )
    }
    items(recipes, key = { it.id }) { recipe ->
        val per = recipe.perServing
        ListItem(
            modifier = Modifier.clickable { onOpen(recipe.id) },
            headlineContent = { Text(recipe.name) },
            supportingContent = {
                Text(
                    stringResource(R.string.feature_nutrition_recipe_summary, formatKcal(per.kcal), formatGrams(per.proteinG), recipe.servings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
internal fun RecipeEditorScreen(
    onBack: () -> Unit,
    onLogServing: (foodId: String) -> Unit,
    onOpenGroceries: () -> Unit = {},
    viewModel: RecipeEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved, uiState.deleted) { if (uiState.saved || uiState.deleted) onBack() }
    RecipeEditorContent(
        uiState = uiState,
        onBack = onBack,
        onName = viewModel::setName,
        onServings = viewModel::setServings,
        onInstructions = viewModel::setInstructions,
        onQuery = viewModel::setIngredientQuery,
        onAdd = viewModel::addIngredient,
        onGrams = viewModel::setIngredientGrams,
        onRemove = viewModel::removeIngredient,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onLogServing = { uiState.foodId?.let(onLogServing) },
        onAddToGroceries = viewModel::addToGroceries,
        onOpenGroceries = onOpenGroceries,
    )
}

@Composable
internal fun RecipeEditorContent(
    uiState: RecipeEditorUiState,
    onBack: () -> Unit,
    onName: (String) -> Unit = {},
    onServings: (Int) -> Unit = {},
    onInstructions: (String) -> Unit = {},
    onQuery: (String) -> Unit = {},
    onAdd: (Food) -> Unit = {},
    onGrams: (Int, String) -> Unit = { _, _ -> },
    onRemove: (Int) -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {},
    onLogServing: () -> Unit = {},
    onAddToGroceries: () -> Unit = {},
    onOpenGroceries: () -> Unit = {},
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val title = when {
        uiState.isNew -> stringResource(R.string.feature_nutrition_new_recipe)
        else -> uiState.name
    }
    Scaffold(
        topBar = {
            BackBar(title, onBack) {
                if (!uiState.isNew && !uiState.isBuiltIn) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(painterResource(JgIcons.Delete), stringResource(R.string.feature_nutrition_delete_recipe))
                    }
                }
            }
        },
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
            if (uiState.isBuiltIn) {
                Text(
                    stringResource(R.string.feature_nutrition_built_in_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onName,
                label = { Text(stringResource(R.string.feature_nutrition_recipe_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.feature_nutrition_servings), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { onServings(uiState.servings - 1) }, enabled = uiState.servings > 1) {
                    Icon(painterResource(JgIcons.Remove), stringResource(R.string.feature_nutrition_fewer_servings))
                }
                Text("${uiState.servings}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onServings(uiState.servings + 1) }) {
                    Icon(painterResource(JgIcons.Add), stringResource(R.string.feature_nutrition_more_servings))
                }
            }
            SummaryCard(uiState)

            Text(
                stringResource(R.string.feature_nutrition_ingredients),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            uiState.ingredients.forEachIndexed { index, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.food.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = row.grams,
                        onValueChange = { onGrams(index, it) },
                        label = { Text(stringResource(R.string.feature_nutrition_unit_g)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(96.dp),
                    )
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(painterResource(JgIcons.Close), stringResource(R.string.feature_nutrition_remove_entry, row.food.name))
                    }
                }
            }
            OutlinedTextField(
                value = uiState.ingredientQuery,
                onValueChange = onQuery,
                label = { Text(stringResource(R.string.feature_nutrition_add_ingredient)) },
                leadingIcon = { Icon(painterResource(JgIcons.Search), null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.ingredientResults.forEach { food ->
                ListItem(
                    modifier = Modifier.clickable { onAdd(food) },
                    headlineContent = { Text(food.name) },
                    supportingContent = { food.brand?.let { Text(it) } },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }

            OutlinedTextField(
                value = uiState.instructions,
                onValueChange = onInstructions,
                label = { Text(stringResource(R.string.feature_nutrition_instructions)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            JgPrimaryButton(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (uiState.isBuiltIn) R.string.feature_nutrition_save_copy else R.string.feature_nutrition_save_recipe,
                    ),
                )
            }
            if (uiState.foodId != null) {
                OutlinedButton(onClick = onLogServing, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_nutrition_log_serving))
                }
            }
            if (uiState.ingredients.isNotEmpty()) {
                OutlinedButton(onClick = onAddToGroceries, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_nutrition_groceries_add_recipe))
                }
                uiState.groceriesAdded?.let { GroceriesAddedNote(it, onOpenGroceries) }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_nutrition_delete_recipe)) },
            text = { Text(stringResource(R.string.feature_nutrition_delete_recipe_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.feature_nutrition_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_nutrition_cancel)) } },
        )
    }
}

@Composable
private fun SummaryCard(uiState: RecipeEditorUiState) {
    val n = uiState.perServing
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.feature_nutrition_per_serving_title, formatGrams(uiState.servingGrams)), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.feature_nutrition_kcal_value, formatKcal(n.kcal)), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.feature_nutrition_macros_line, formatGrams(n.proteinG), formatGrams(n.carbsG), formatGrams(n.fatG)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
