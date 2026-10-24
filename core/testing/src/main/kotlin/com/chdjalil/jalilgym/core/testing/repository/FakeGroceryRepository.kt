package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.nutrition.GroceryLine
import com.chdjalil.jalilgym.core.domain.repository.GroceryRepository
import com.chdjalil.jalilgym.core.model.GroceryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeGroceryRepository : GroceryRepository {
    val items = MutableStateFlow<List<GroceryItem>>(emptyList())
    private var nextId = 0

    override fun observeItems(): Flow<List<GroceryItem>> = items.map { all -> all.sortedBy { it.checked } }

    override suspend fun addLines(lines: List<GroceryLine>): Int {
        lines.forEach { line ->
            items.update { all ->
                val existing = all.firstOrNull { !it.checked && it.foodId != null && it.foodId == line.foodId }
                if (existing != null) {
                    all.map { if (it.id == existing.id) it.copy(grams = sum(it.grams, line.grams)) else it }
                } else {
                    all + GroceryItem("g${nextId++}", line.name, line.foodId, line.grams, checked = false)
                }
            }
        }
        return lines.size
    }

    private fun sum(a: Double?, b: Double?): Double? = if (a == null && b == null) null else (a ?: 0.0) + (b ?: 0.0)

    override suspend fun addItem(name: String) {
        items.update { it + GroceryItem("g${nextId++}", name, null, null, checked = false) }
    }

    override suspend fun setChecked(id: String, checked: Boolean) =
        items.update { all -> all.map { if (it.id == id) it.copy(checked = checked) else it } }

    override suspend fun delete(id: String) = items.update { all -> all.filterNot { it.id == id } }

    override suspend fun clearChecked() = items.update { all -> all.filterNot { it.checked } }
}
