package com.chdjalil.jalilgym.core.data.repository

import androidx.room.withTransaction
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.dao.GroceryDao
import com.chdjalil.jalilgym.core.database.entity.GroceryItemEntity
import com.chdjalil.jalilgym.core.domain.nutrition.GroceryLine
import com.chdjalil.jalilgym.core.domain.nutrition.mergeGroceryLines
import com.chdjalil.jalilgym.core.domain.repository.GroceryRepository
import com.chdjalil.jalilgym.core.model.GroceryItem
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class OfflineFirstGroceryRepository @Inject constructor(
    private val database: JalilgymDatabase,
    private val groceryDao: GroceryDao,
) : GroceryRepository {

    override fun observeItems(): Flow<List<GroceryItem>> =
        groceryDao.observeItems().map { list -> list.map { GroceryItem(it.id, it.name, it.foodId, it.grams, it.checked) } }

    override suspend fun addLines(lines: List<GroceryLine>): Int {
        val merged = mergeGroceryLines(lines.filter { it.name.isNotBlank() })
        database.withTransaction {
            // Created one millisecond apart so the list keeps the order lines were given in.
            val now = System.currentTimeMillis()
            merged.forEachIndexed { index, line ->
                val existing = if (line.foodId != null) groceryDao.openItemFor(line.foodId) else groceryDao.openItemNamed(line.key)
                if (existing != null) {
                    val grams = if (existing.grams == null && line.grams == null) null else (existing.grams ?: 0.0) + (line.grams ?: 0.0)
                    groceryDao.upsert(existing.copy(grams = grams, updatedAt = now))
                } else {
                    groceryDao.upsert(
                        GroceryItemEntity(
                            id = UUID.randomUUID().toString(),
                            name = line.name.trim(),
                            foodId = line.foodId,
                            grams = line.grams,
                            checked = false,
                            createdAt = now + index,
                            updatedAt = now,
                        ),
                    )
                }
            }
        }
        return merged.size
    }

    override suspend fun addItem(name: String) {
        addLines(listOf(GroceryLine(name, foodId = null, grams = null)))
    }

    override suspend fun setChecked(id: String, checked: Boolean) = groceryDao.setChecked(id, checked, System.currentTimeMillis())

    override suspend fun delete(id: String) = groceryDao.delete(id)

    override suspend fun clearChecked() = groceryDao.deleteChecked()
}
