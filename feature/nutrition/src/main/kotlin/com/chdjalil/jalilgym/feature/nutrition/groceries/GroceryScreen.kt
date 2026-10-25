package com.chdjalil.jalilgym.feature.nutrition.groceries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chdjalil.jalilgym.core.designsystem.icon.JgIcons
import com.chdjalil.jalilgym.core.model.GroceryItem
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.nutrition.R
import com.chdjalil.jalilgym.feature.nutrition.ui.BackBar
import com.chdjalil.jalilgym.feature.nutrition.ui.formatGroceryAmount

@Composable
internal fun GroceryScreen(onBack: () -> Unit, viewModel: GroceryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GroceryContent(
        uiState = uiState,
        onBack = onBack,
        onAdd = viewModel::add,
        onChecked = viewModel::setChecked,
        onDelete = viewModel::delete,
        onClearChecked = viewModel::clearChecked,
    )
}

@Composable
internal fun GroceryContent(
    uiState: GroceryUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit = {},
    onChecked: (String, Boolean) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onClearChecked: () -> Unit = {},
) {
    var newItem by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(newItem.trim())
        newItem = ""
    }
    Scaffold(
        topBar = {
            BackBar(stringResource(R.string.feature_nutrition_groceries), onBack) {
                if (uiState.inCart.isNotEmpty()) {
                    TextButton(onClick = onClearChecked) { Text(stringResource(R.string.feature_nutrition_groceries_clear)) }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text(stringResource(R.string.feature_nutrition_groceries_add)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (newItem.isNotBlank()) submit() }),
                    trailingIcon = {
                        if (newItem.isNotBlank()) {
                            IconButton(onClick = submit) {
                                Icon(painterResource(JgIcons.Add), stringResource(R.string.feature_nutrition_groceries_add))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!uiState.isLoading && uiState.toBuy.isEmpty() && uiState.inCart.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_nutrition_groceries_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            groceryGroup(R.string.feature_nutrition_groceries_to_buy, uiState.toBuy, uiState.units, onChecked, onDelete)
            groceryGroup(R.string.feature_nutrition_groceries_in_cart, uiState.inCart, uiState.units, onChecked, onDelete)
        }
    }
}

private fun LazyListScope.groceryGroup(
    title: Int,
    items: List<GroceryItem>,
    units: UnitSystem,
    onChecked: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "title-$title") {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 12.dp)
                .semantics { heading() },
        )
    }
    items(items, key = { it.id }) { item -> GroceryRow(item, units, onChecked, onDelete) }
}

@Composable
private fun GroceryRow(item: GroceryItem, units: UnitSystem, onChecked: (String, Boolean) -> Unit, onDelete: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .toggleable(value = item.checked, role = Role.Checkbox, onValueChange = { onChecked(item.id, it) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.checked, onCheckedChange = null)
            Box(Modifier.padding(start = 12.dp)) {
                Text(
                    text = item.grams?.let { "${item.name} · ${formatGroceryAmount(it, units)}" } ?: item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        IconButton(onClick = { onDelete(item.id) }) {
            Icon(painterResource(JgIcons.Close), stringResource(R.string.feature_nutrition_remove_entry, item.name))
        }
    }
}

/** "5 items added to your grocery list · View". */
@Composable
internal fun GroceriesAddedNote(count: Int, onOpen: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            pluralStringResource(R.plurals.feature_nutrition_groceries_added, count, count),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpen) { Text(stringResource(R.string.feature_nutrition_groceries_view)) }
    }
}
