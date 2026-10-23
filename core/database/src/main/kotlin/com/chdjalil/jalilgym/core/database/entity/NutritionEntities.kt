package com.chdjalil.jalilgym.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.DietStyle
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.model.MealType
import java.time.LocalDate

@Entity(tableName = "diet_plans", indices = [Index("isActive")])
data class DietPlanEntity(
    @PrimaryKey val id: String,
    val goal: DietGoal,
    val style: DietStyle,
    val halalOnly: Boolean,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val isManualOverride: Boolean,
    val startDate: LocalDate,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Nutrients are per 100 g; micronutrients are optional because sources often omit them. */
@Entity(
    tableName = "foods",
    indices = [Index("barcode"), Index("name"), Index("lastUsedAt")],
)
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val source: FoodSource,
    val offCode: String?,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double?,
    val sugarPer100g: Double?,
    val satFatPer100g: Double?,
    val sodiumMgPer100g: Double?,
    val halalStatus: HalalStatus,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long?,
    /** When this row was last refreshed from Open Food Facts. */
    val cachedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "food_servings",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("foodId")],
)
data class FoodServingEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val label: String,
    val grams: Double,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val servings: Int,
    val mealTags: List<String>,
    val dietTags: List<String>,
    val isHalal: Boolean,
    val isBuiltIn: Boolean,
    val instructions: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["recipeId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("foodId")],
)
data class RecipeIngredientEntity(
    val recipeId: String,
    val position: Int,
    val foodId: String,
    val grams: Double,
)

/**
 * A diary entry. Macros are a snapshot taken when logging, so editing or
 * re-caching a food never rewrites past days.
 */
@Entity(
    tableName = "food_log_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("date"), Index("foodId"), Index("recipeId")],
)
data class FoodLogEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val meal: MealType,
    val foodId: String?,
    val recipeId: String?,
    val quantityG: Double,
    val nameSnapshot: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double?,
    val sugarG: Double?,
    val sodiumMg: Double?,
    val loggedAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "saved_meals")
data class SavedMealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultMeal: MealType?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "saved_meal_items",
    primaryKeys = ["savedMealId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SavedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedMealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("foodId")],
)
data class SavedMealItemEntity(
    val savedMealId: String,
    val position: Int,
    val foodId: String,
    val quantityG: Double,
)

@Entity(
    tableName = "meal_plan_entries",
    foreignKeys = [
        ForeignKey(
            entity = DietPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["dietPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dietPlanId", "date"), Index("recipeId")],
)
data class MealPlanEntryEntity(
    @PrimaryKey val id: String,
    val dietPlanId: String,
    val date: LocalDate,
    val meal: MealType,
    val recipeId: String,
    val servings: Double,
)
