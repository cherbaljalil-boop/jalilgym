package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {

    @Query("SELECT * FROM grocery_items WHERE deletedAt IS NULL ORDER BY checked, createdAt")
    fun observeItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items WHERE foodId = :foodId AND checked = 0 AND deletedAt IS NULL LIMIT 1")
    suspend fun openItemFor(foodId: String): GroceryItemEntity?

    @Query("SELECT * FROM grocery_items WHERE foodId IS NULL AND checked = 0 AND deletedAt IS NULL AND lower(trim(name)) = :key LIMIT 1")
    suspend fun openItemNamed(key: String): GroceryItemEntity?

    @Upsert
    suspend fun upsert(item: GroceryItemEntity)

    @Query("UPDATE grocery_items SET checked = :checked, updatedAt = :now WHERE id = :id")
    suspend fun setChecked(id: String, checked: Boolean, now: Long)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM grocery_items WHERE checked = 1")
    suspend fun deleteChecked()
}
