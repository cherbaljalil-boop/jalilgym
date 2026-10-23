package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.FoodDao
import com.chdjalil.jalilgym.core.database.entity.FoodEntity
import com.chdjalil.jalilgym.core.database.entity.FoodServingEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedFood
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.nutrition.halalStatusOf
import com.chdjalil.jalilgym.core.domain.repository.BarcodeLookup
import com.chdjalil.jalilgym.core.domain.repository.FoodRepository
import com.chdjalil.jalilgym.core.domain.repository.OnlineSearch
import com.chdjalil.jalilgym.core.model.Food
import com.chdjalil.jalilgym.core.model.FoodDraft
import com.chdjalil.jalilgym.core.model.FoodSource
import com.chdjalil.jalilgym.core.model.HalalStatus
import com.chdjalil.jalilgym.core.network.OffProduct
import com.chdjalil.jalilgym.core.network.OpenFoodFactsDataSource
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cached Open Food Facts products use a stable id derived from their barcode. */
internal fun offFoodId(code: String) = "off-$code"

internal class OfflineFirstFoodRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val openFoodFacts: OpenFoodFactsDataSource,
) : FoodRepository {

    private fun now() = System.currentTimeMillis()

    private fun Flow<List<PopulatedFood>>.models() = map { list -> list.map { it.asExternalModel() } }

    override fun searchLocal(query: String): Flow<List<Food>> = foodDao.search(query.trim()).models()

    override fun observeRecent(): Flow<List<Food>> = foodDao.observeRecent().models()

    override fun observeFavorites(): Flow<List<Food>> = foodDao.observeFavorites().models()

    override fun observeFood(id: String): Flow<Food?> = foodDao.observeFood(id).map { it?.asExternalModel() }

    override suspend fun searchOnline(query: String): OnlineSearch {
        val products = try {
            openFoodFacts.search(query.trim())
        } catch (e: IOException) {
            return OnlineSearch.Offline
        }
        val cached = cache(products.filter { it.isLoggable })
        return OnlineSearch.Results(cached.mapNotNull { foodDao.getFood(it)?.asExternalModel() })
    }

    override suspend fun lookupBarcode(barcode: String): BarcodeLookup {
        val code = barcode.trim()
        foodDao.getByBarcode(code)?.let { return BarcodeLookup.Found(it.asExternalModel()) }
        val product = try {
            openFoodFacts.product(code)
        } catch (e: IOException) {
            return BarcodeLookup.Offline(code)
        }
        if (product == null || !product.isLoggable) return BarcodeLookup.NotFound(code)
        val id = cache(listOf(product)).single()
        return BarcodeLookup.Found(checkNotNull(foodDao.getFood(id)).asExternalModel())
    }

    /**
     * Upserts products into the cache, keeping what the user set on them
     * (favorite, last used, their own halal mark). Returns the food ids in order.
     */
    private suspend fun cache(products: List<OffProduct>): List<String> {
        if (products.isEmpty()) return emptyList()
        val now = now()
        val existing = foodDao.getByOffCodes(products.map { it.code }).associateBy { it.offCode }
        val foods = products.map { product ->
            val old = existing[product.code]
            val detected = halalStatusOf(product.labelTags, product.ingredientsText)
            FoodEntity(
                id = old?.id ?: offFoodId(product.code),
                name = product.name,
                brand = product.brand,
                barcode = product.code,
                source = FoodSource.OPEN_FOOD_FACTS,
                offCode = product.code,
                kcalPer100g = product.kcal ?: 0.0,
                proteinPer100g = product.proteinG ?: 0.0,
                carbsPer100g = product.carbsG ?: 0.0,
                fatPer100g = product.fatG ?: 0.0,
                fiberPer100g = product.fiberG,
                sugarPer100g = product.sugarG,
                satFatPer100g = product.satFatG,
                sodiumMgPer100g = product.sodiumMg,
                halalStatus = if (old?.halalStatus == HalalStatus.USER_MARKED && detected == HalalStatus.UNKNOWN) {
                    HalalStatus.USER_MARKED
                } else {
                    detected
                },
                isFavorite = old?.isFavorite ?: false,
                lastUsedAt = old?.lastUsedAt,
                cachedAt = now,
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
            )
        }
        val servings = products.zip(foods).mapNotNull { (product, food) ->
            val grams = product.servingGrams ?: return@mapNotNull null
            FoodServingEntity("${food.id}-serving", food.id, product.servingLabel ?: "1 serving", grams)
        }
        foodDao.replaceFoods(foods, servings)
        return foods.map { it.id }
    }

    override suspend fun saveCustomFood(draft: FoodDraft): String {
        val now = now()
        val old = draft.id?.let { foodDao.getFood(it)?.food }
        val id = old?.id ?: UUID.randomUUID().toString()
        val n = draft.per100g
        val food = FoodEntity(
            id = id,
            name = draft.name.trim(),
            brand = draft.brand?.trim()?.takeIf { it.isNotEmpty() },
            barcode = draft.barcode?.trim()?.takeIf { it.isNotEmpty() },
            source = FoodSource.CUSTOM,
            offCode = null,
            kcalPer100g = n.kcal,
            proteinPer100g = n.proteinG,
            carbsPer100g = n.carbsG,
            fatPer100g = n.fatG,
            fiberPer100g = n.fiberG,
            sugarPer100g = n.sugarG,
            satFatPer100g = n.satFatG,
            sodiumMgPer100g = n.sodiumMg,
            halalStatus = if (draft.markedHalal) HalalStatus.USER_MARKED else HalalStatus.UNKNOWN,
            isFavorite = old?.isFavorite ?: false,
            lastUsedAt = old?.lastUsedAt,
            cachedAt = null,
            createdAt = old?.createdAt ?: now,
            updatedAt = now,
        )
        val servings = draft.servings.filter { it.grams > 0 }.mapIndexed { index, serving ->
            FoodServingEntity("$id-serving-$index", id, serving.label.trim(), serving.grams)
        }
        foodDao.replaceFoods(listOf(food), servings)
        return id
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = foodDao.setFavorite(id, favorite, now())

    override suspend fun setUserMarkedHalal(id: String, marked: Boolean) =
        foodDao.setHalalStatus(id, if (marked) HalalStatus.USER_MARKED else HalalStatus.UNKNOWN, now())
}
