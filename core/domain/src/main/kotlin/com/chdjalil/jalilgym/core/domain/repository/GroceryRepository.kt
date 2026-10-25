package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.domain.nutrition.GroceryLine
import com.chdjalil.jalilgym.core.model.GroceryItem
import kotlinx.coroutines.flow.Flow

interface GroceryRepository {
    /** Unchecked items first, each group in the order added. */
    fun observeItems(): Flow<List<GroceryItem>>

    /** Adds [lines], topping up unchecked items for the same food; returns how many lines were added or topped up. */
    suspend fun addLines(lines: List<GroceryLine>): Int

    suspend fun addItem(name: String)

    suspend fun setChecked(id: String, checked: Boolean)

    suspend fun delete(id: String)

    suspend fun clearChecked()
}
