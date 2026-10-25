package com.chdjalil.jalilgym.core.database.seed

import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.FoodServingEntity
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import kotlinx.serialization.Serializable

/** Shape of `assets/seed/foods.json`: generic foods that work offline. Bump [version] on changes. */
@Serializable
data class FoodSeedFile(
    val version: Int,
    val source: String? = null,
    val foods: List<SeedFood>,
)

/** Nutrients per 100 g. */
@Serializable
data class SeedFood(
    val id: String,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val satFat: Double? = null,
    val sodiumMg: Double? = null,
    val servings: List<SeedServing> = emptyList(),
)

@Serializable
data class SeedServing(val label: String, val grams: Double)

/** Keeps the user's favorite flag and last-used time when re-importing. */
internal fun SeedFood.toEntity(now: Long, existing: FoodEntity?) = FoodEntity(
    id = id,
    name = name,
    brand = null,
    barcode = null,
    source = FoodSource.SEED,
    offCode = null,
    kcalPer100g = kcal,
    proteinPer100g = protein,
    carbsPer100g = carbs,
    fatPer100g = fat,
    fiberPer100g = fiber,
    sugarPer100g = sugar,
    satFatPer100g = satFat,
    sodiumMgPer100g = sodiumMg,
    // Generic foods carry no label, so they're never shown as halal unless the user marks them.
    halalStatus = existing?.halalStatus ?: HalalStatus.UNKNOWN,
    isFavorite = existing?.isFavorite ?: false,
    lastUsedAt = existing?.lastUsedAt,
    cachedAt = null,
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
)

internal fun SeedFood.servingEntities() = servings.mapIndexed { index, serving ->
    FoodServingEntity("$id-serving-$index", id, serving.label, serving.grams)
}
