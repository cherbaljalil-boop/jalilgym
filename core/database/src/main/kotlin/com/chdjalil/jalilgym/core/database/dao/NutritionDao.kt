package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.DietPlanEntity
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.FoodLogEntryEntity
import com.chdjalil.jalilgym.core.database.entity.FoodServingEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealEntity
import com.chdjalil.jalilgym.core.database.entity.SavedMealItemEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedFood
import com.chdjalil.jalilgym.core.database.model.PopulatedSavedMeal
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MealType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Daily calorie and protein totals, for streaks and target badges. */
data class DailyTotalsRow(val date: LocalDate, val kcal: Double, val proteinG: Double)

@Dao
interface FoodDao {
    @Transaction
    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeFood(id: String): Flow<PopulatedFood?>

    @Transaction
    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFood(id: String): PopulatedFood?

    @Transaction
    @Query("SELECT * FROM foods WHERE barcode = :barcode AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getByBarcode(barcode: String): PopulatedFood?

    @Query("SELECT * FROM foods WHERE offCode IN (:codes)")
    suspend fun getByOffCodes(codes: List<String>): List<FoodEntity>

    /** Name or brand contains [query]; favorites and recently used first. */
    @Transaction
    @Query(
        """
        SELECT * FROM foods
        WHERE deletedAt IS NULL AND (name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%')
        ORDER BY isFavorite DESC, (lastUsedAt IS NULL), lastUsedAt DESC, name
        LIMIT :limit
        """,
    )
    fun search(query: String, limit: Int = 50): Flow<List<PopulatedFood>>

    @Transaction
    @Query("SELECT * FROM foods WHERE deletedAt IS NULL AND lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<PopulatedFood>>

    @Transaction
    @Query("SELECT * FROM foods WHERE deletedAt IS NULL AND isFavorite = 1 ORDER BY name")
    fun observeFavorites(): Flow<List<PopulatedFood>>

    @Upsert
    suspend fun upsertFoods(foods: List<FoodEntity>)

    @Query("DELETE FROM food_servings WHERE foodId IN (:foodIds)")
    suspend fun deleteServingsFor(foodIds: List<String>)

    @Insert
    suspend fun insertServings(servings: List<FoodServingEntity>)

    @Transaction
    suspend fun replaceFoods(foods: List<FoodEntity>, servings: List<FoodServingEntity>) {
        upsertFoods(foods)
        deleteServingsFor(foods.map { it.id })
        insertServings(servings)
    }

    @Query("UPDATE foods SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE foods SET halalStatus = :status, updatedAt = :now WHERE id = :id")
    suspend fun setHalalStatus(id: String, status: HalalStatus, now: Long)

    @Query("UPDATE foods SET lastUsedAt = :now WHERE id = :id")
    suspend fun markUsed(id: String, now: Long)

    @Query("UPDATE foods SET deletedAt = :now, updatedAt = :now WHERE id = :id AND source = 'CUSTOM'")
    suspend fun softDeleteCustom(id: String, now: Long)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_log_entries WHERE date = :date AND deletedAt IS NULL ORDER BY loggedAt")
    fun observeDay(date: LocalDate): Flow<List<FoodLogEntryEntity>>

    @Query("SELECT * FROM food_log_entries WHERE date = :date AND meal = :meal AND deletedAt IS NULL ORDER BY loggedAt")
    suspend fun entriesFor(date: LocalDate, meal: MealType): List<FoodLogEntryEntity>

    @Query("SELECT * FROM food_log_entries WHERE id = :id")
    suspend fun getEntry(id: String): FoodLogEntryEntity?

    @Upsert
    suspend fun upsertEntries(entries: List<FoodLogEntryEntity>)

    @Query("DELETE FROM food_log_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("SELECT COUNT(*) FROM food_log_entries WHERE deletedAt IS NULL")
    fun observeEntryCount(): Flow<Int>

    @Query(
        """
        SELECT date, SUM(kcal) AS kcal, SUM(proteinG) AS proteinG FROM food_log_entries
        WHERE deletedAt IS NULL GROUP BY date ORDER BY date
        """,
    )
    fun observeDailyTotals(): Flow<List<DailyTotalsRow>>
}

@Dao
interface SavedMealDao {
    @Transaction
    @Query("SELECT * FROM saved_meals WHERE deletedAt IS NULL ORDER BY name")
    fun observeAll(): Flow<List<PopulatedSavedMeal>>

    @Transaction
    @Query("SELECT * FROM saved_meals WHERE id = :id")
    suspend fun get(id: String): PopulatedSavedMeal?

    @Insert
    suspend fun insertMeal(meal: SavedMealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SavedMealItemEntity>)

    @Transaction
    suspend fun insertMealWithItems(meal: SavedMealEntity, items: List<SavedMealItemEntity>) {
        insertMeal(meal)
        insertItems(items)
    }

    @Query("DELETE FROM saved_meals WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DietPlanDao {
    @Query("SELECT * FROM diet_plans WHERE isActive = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    fun observeActive(): Flow<DietPlanEntity?>

    @Query("UPDATE diet_plans SET isActive = 0, updatedAt = :now WHERE isActive = 1")
    suspend fun deactivateAll(now: Long)

    @Upsert
    suspend fun upsert(plan: DietPlanEntity)

    @Transaction
    suspend fun activate(plan: DietPlanEntity) {
        deactivateAll(plan.updatedAt)
        upsert(plan.copy(isActive = true))
    }
}
